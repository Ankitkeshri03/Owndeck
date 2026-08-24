package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.dto.product.ExtractedInvoiceData;
import com.owndeck.invoicewarranty.dto.product.ProductAlertResponse;
import com.owndeck.invoicewarranty.dto.product.ProductCreateRequest;
import com.owndeck.invoicewarranty.dto.product.ProductResponse;
import com.owndeck.invoicewarranty.dto.product.ProductUpdateRequest;
import com.owndeck.invoicewarranty.dto.product.ProductUploadRequest;
import com.owndeck.invoicewarranty.entity.ProductInvoice;
import com.owndeck.invoicewarranty.entity.User;
import com.owndeck.invoicewarranty.entity.enums.ProductStatus;
import com.owndeck.invoicewarranty.exception.BadRequestException;
import com.owndeck.invoicewarranty.exception.NotFoundException;
import com.owndeck.invoicewarranty.repository.ProductInvoiceRepository;
import com.owndeck.invoicewarranty.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {
  private static final int PRODUCT_NAME_MAX_LENGTH = 255;
  private static final int DESCRIPTION_MAX_LENGTH = 1200;

  private final ProductInvoiceRepository productRepository;
  private final UserRepository userRepository;
  private final FileStorageService fileStorageService;
  private final OcrService ocrService;
  private final InvoiceExtractionService invoiceExtractionService;

  public ProductService(
      ProductInvoiceRepository productRepository,
      UserRepository userRepository,
      FileStorageService fileStorageService,
      OcrService ocrService,
      InvoiceExtractionService invoiceExtractionService
  ) {
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
    this.ocrService = ocrService;
    this.invoiceExtractionService = invoiceExtractionService;
  }

  @Transactional
  public ProductResponse uploadProduct(
      Long userId,
      MultipartFile file,
      ProductUploadRequest request
  ) {
    User user = getUser(userId);
    FileStorageService.StoredFile storedFile = fileStorageService.storeInvoice(file);
    String ocrText = ocrService.extractText(storedFile.path(), storedFile.contentType());
    ExtractedInvoiceData extracted = invoiceExtractionService.extract(ocrText);

    ProductInvoice product = new ProductInvoice();
    product.setUser(user);
    product.setProductName(limit(firstNonBlank(
        request.getProductName(),
        extracted.getProductName(),
        "Unnamed Product"
    ), PRODUCT_NAME_MAX_LENGTH));
    product.setTitleDescription(limit(firstNonBlank(
        request.getTitleDescription(),
        extracted.getTitleDescription(),
        product.getProductName()
    ), DESCRIPTION_MAX_LENGTH));
    product.setQuantity(firstPositive(request.getQuantity(), extracted.getQuantity(), 1));

    LocalDate purchaseDate = firstNonNull(request.getPurchaseDate(), extracted.getPurchaseDate(), LocalDate.now());
    int warrantyMonths = sanitizeMonths(firstNonNull(
        request.getWarrantyDurationMonths(),
        extracted.getWarrantyDurationMonths(),
        12
    ));
    LocalDate expiryDate = firstNonNull(
        request.getExpiryDate(),
        extracted.getExpiryDate(),
        purchaseDate.plusMonths(warrantyMonths)
    );

    product.setPurchaseDate(purchaseDate);
    product.setWarrantyDurationMonths(warrantyMonths);
    product.setExpiryDate(expiryDate);
    product.setStatus(resolveStatus(expiryDate));
    product.setInvoiceUrl(storedFile.url());
    product.setInvoicePath(storedFile.path().toString());
    product.setOcrText(ocrText == null ? "" : ocrText);

    ProductInvoice saved = productRepository.save(product);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> listProducts(Long userId, String status, String filter, String search) {
    String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    ProductStatus statusFilter = parseStatusFilter(status, filter);

    return productRepository.findByUser_IdOrderByCreatedAtDesc(userId)
        .stream()
        .filter(p -> statusFilter == null || resolveStatus(p.getExpiryDate()) == statusFilter)
        .filter(p -> normalizedSearch.isBlank() || matchesSearch(p, normalizedSearch))
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ProductResponse getProduct(Long userId, Long productId) {
    ProductInvoice product = findOwnedProduct(userId, productId);
    return toResponse(product);
  }

  @Transactional
  public ProductResponse createProduct(Long userId, ProductCreateRequest request) {
    User user = getUser(userId);

    LocalDate purchaseDate = request.purchaseDate();
    int warrantyMonths = sanitizeMonths(firstNonNull(request.warrantyDurationMonths(), 12));
    LocalDate expiryDate = firstNonNull(
        request.expiryDate(),
        purchaseDate.plusMonths(warrantyMonths)
    );

    ProductInvoice product = new ProductInvoice();
    product.setUser(user);
    product.setProductName(limit(request.productName().trim(), PRODUCT_NAME_MAX_LENGTH));
    product.setTitleDescription(limit(
        firstNonBlank(request.titleDescription(), request.productName()),
        DESCRIPTION_MAX_LENGTH
    ));
    product.setQuantity(firstPositive(request.quantity(), 1));
    product.setPurchaseDate(purchaseDate);
    product.setWarrantyDurationMonths(warrantyMonths);
    product.setExpiryDate(expiryDate);
    product.setStatus(resolveStatus(expiryDate));
    product.setInvoiceUrl(null);
    product.setInvoicePath(null);
    product.setOcrText("");

    ProductInvoice saved = productRepository.save(product);
    return toResponse(saved);
  }

  @Transactional
  public ProductResponse updateProduct(Long userId, Long productId, ProductUpdateRequest request) {
    ProductInvoice product = findOwnedProduct(userId, productId);

    if (request.productName() != null && !request.productName().isBlank()) {
      product.setProductName(limit(request.productName().trim(), PRODUCT_NAME_MAX_LENGTH));
    }
    if (request.titleDescription() != null) {
      product.setTitleDescription(limit(request.titleDescription().trim(), DESCRIPTION_MAX_LENGTH));
    }
    if (request.quantity() != null) {
      product.setQuantity(firstPositive(request.quantity(), product.getQuantity()));
    }

    LocalDate nextPurchaseDate = firstNonNull(request.purchaseDate(), product.getPurchaseDate());
    int nextWarrantyMonths = sanitizeMonths(firstNonNull(
        request.warrantyDurationMonths(),
        product.getWarrantyDurationMonths(),
        0
    ));

    LocalDate nextExpiryDate;
    if (request.expiryDate() != null) {
      nextExpiryDate = request.expiryDate();
    } else if (request.purchaseDate() != null || request.warrantyDurationMonths() != null) {
      nextExpiryDate = nextPurchaseDate.plusMonths(nextWarrantyMonths);
    } else {
      nextExpiryDate = product.getExpiryDate();
    }

    product.setPurchaseDate(nextPurchaseDate);
    product.setWarrantyDurationMonths(nextWarrantyMonths);
    product.setExpiryDate(nextExpiryDate);
    product.setStatus(resolveStatus(nextExpiryDate));

    ProductInvoice saved = productRepository.save(product);
    return toResponse(saved);
  }

  @Transactional
  public void deleteProduct(Long userId, Long productId) {
    ProductInvoice product = findOwnedProduct(userId, productId);
    String invoicePath = product.getInvoicePath();
    productRepository.delete(product);

    if (invoicePath != null && !invoicePath.isBlank()) {
      try {
        Files.deleteIfExists(Path.of(invoicePath));
      } catch (IOException ignored) {
        // File cleanup failures should not block product deletion.
      }
    }
  }

  @Transactional(readOnly = true)
  public List<ProductAlertResponse> getAlerts(Long userId) {
    LocalDate today = LocalDate.now();
    return productRepository.findByUser_Id(userId)
        .stream()
        .map(product -> {
          int days = daysRemaining(product.getExpiryDate(), today);
          if (days > 30) {
            return null;
          }
          String productName = product.getProductName() == null || product.getProductName().isBlank()
              ? "Your product"
              : product.getProductName().trim();
          String alertType;
          String message;

          if (days < 0) {
            alertType = "EXPIRED";
            message = productName + " warranty has expired.";
          } else if (days == 0) {
            alertType = "EXPIRING_TODAY";
            message = productName + " warranty expires today.";
          } else if (days == 1) {
            alertType = "EXPIRING_IN_ONE_DAY";
            message = productName + " warranty expires tomorrow.";
          } else if (days <= 7) {
            alertType = "EXPIRING_IN_ONE_WEEK";
            message = productName + " warranty expires in " + days + " days.";
          } else {
            alertType = "EXPIRING_SOON";
            message = productName + " warranty expires in " + days + " days.";
          }
          return new ProductAlertResponse(
              product.getId(),
              productName,
              product.getExpiryDate(),
              days,
              alertType,
              message
          );
        })
        .filter(java.util.Objects::nonNull)
        .sorted(Comparator.comparing(ProductAlertResponse::daysRemaining))
        .toList();
  }

  @Transactional(readOnly = true)
  public Resource getInvoiceResource(Long userId, Long productId) {
    ProductInvoice product = findOwnedProduct(userId, productId);
    if (product.getInvoicePath() == null || product.getInvoicePath().isBlank()) {
      throw new BadRequestException("No invoice file is attached to this product.");
    }
    return fileStorageService.loadAsResource(product.getInvoicePath());
  }

  private ProductInvoice findOwnedProduct(Long userId, Long productId) {
    return productRepository.findByIdAndUser_Id(productId, userId)
        .orElseThrow(() -> new NotFoundException("Product not found."));
  }

  private User getUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found."));
  }

  private ProductResponse toResponse(ProductInvoice product) {
    ProductStatus status = resolveStatus(product.getExpiryDate());
    ProductResponse response = new ProductResponse();
    response.setId(product.getId());
    response.setUserId(product.getUser().getId());
    response.setProductName(product.getProductName());
    response.setTitleDescription(product.getTitleDescription());
    response.setQuantity(product.getQuantity());
    response.setPurchaseDate(product.getPurchaseDate());
    response.setExpiryDate(product.getExpiryDate());
    response.setWarrantyDurationMonths(product.getWarrantyDurationMonths());
    response.setInvoiceUrl(product.getInvoiceUrl());
    response.setStatus(toStatusLabel(status));
    response.setDaysRemaining(daysRemaining(product.getExpiryDate(), LocalDate.now()));
    response.setRawOcrText(product.getOcrText());
    response.setCreatedAt(product.getCreatedAt());
    response.setUpdatedAt(product.getUpdatedAt());
    return response;
  }

  private ProductStatus resolveStatus(LocalDate expiryDate) {
    int days = daysRemaining(expiryDate, LocalDate.now());
    if (days < 0) {
      return ProductStatus.EXPIRED;
    }
    if (days <= 30) {
      return ProductStatus.EXPIRING_SOON;
    }
    return ProductStatus.ACTIVE;
  }

  private int daysRemaining(LocalDate expiryDate, LocalDate now) {
    return (int) ChronoUnit.DAYS.between(now, expiryDate);
  }

  private boolean matchesSearch(ProductInvoice product, String search) {
    String name = product.getProductName() == null ? "" : product.getProductName().toLowerCase(Locale.ROOT);
    String description = product.getTitleDescription() == null ? "" : product.getTitleDescription().toLowerCase(
        Locale.ROOT);
    return name.contains(search) || description.contains(search);
  }

  private ProductStatus parseStatusFilter(String status, String filter) {
    if (status != null && !status.isBlank()) {
      return parseStatusValue(status);
    }
    if (filter == null || filter.isBlank()) {
      return null;
    }
    String normalized = filter.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "active" -> ProductStatus.ACTIVE;
      case "expired" -> ProductStatus.EXPIRED;
      case "expiring", "expiringsoon", "expiring_soon" -> ProductStatus.EXPIRING_SOON;
      default -> null;
    };
  }

  private ProductStatus parseStatusValue(String value) {
    String normalized = value.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    return switch (normalized) {
      case "active" -> ProductStatus.ACTIVE;
      case "expired" -> ProductStatus.EXPIRED;
      case "expiring_soon", "expiringsoon", "expiring" -> ProductStatus.EXPIRING_SOON;
      default -> null;
    };
  }

  private String toStatusLabel(ProductStatus status) {
    return switch (status) {
      case ACTIVE -> "Active";
      case EXPIRED -> "Expired";
      case EXPIRING_SOON -> "Expiring Soon";
    };
  }

  @SafeVarargs
  private <T> T firstNonNull(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private String limit(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    return trimmed.substring(0, maxLength).trim();
  }

  private int firstPositive(Integer... values) {
    for (Integer value : values) {
      if (value != null && value > 0) {
        return value;
      }
    }
    return 1;
  }

  private int sanitizeMonths(Integer months) {
    if (months == null || months < 0) {
      return 0;
    }
    return Math.min(months, 240);
  }
}
