package com.owndeck.invoicewarranty.controller;

import com.owndeck.invoicewarranty.dto.product.ProductAlertResponse;
import com.owndeck.invoicewarranty.dto.product.ProductCreateRequest;
import com.owndeck.invoicewarranty.dto.product.ProductResponse;
import com.owndeck.invoicewarranty.dto.product.ProductUpdateRequest;
import com.owndeck.invoicewarranty.dto.product.ProductUploadRequest;
import com.owndeck.invoicewarranty.exception.UnauthorizedException;
import com.owndeck.invoicewarranty.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public List<ProductResponse> listProducts(
      Authentication authentication,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String search
  ) {
    return productService.listProducts(currentUserId(authentication), status, filter, search);
  }

  @GetMapping("/alerts")
  public List<ProductAlertResponse> alerts(Authentication authentication) {
    return productService.getAlerts(currentUserId(authentication));
  }

  @PostMapping
  public ProductResponse createProduct(
      Authentication authentication,
      @Valid @org.springframework.web.bind.annotation.RequestBody ProductCreateRequest request
  ) {
    return productService.createProduct(currentUserId(authentication), request);
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProductResponse uploadProduct(
      Authentication authentication,
      @RequestParam("file") MultipartFile file,
      @Valid @ModelAttribute ProductUploadRequest request
  ) {
    return productService.uploadProduct(currentUserId(authentication), file, request);
  }

  @GetMapping("/{id}")
  public ProductResponse getProduct(Authentication authentication, @PathVariable Long id) {
    return productService.getProduct(currentUserId(authentication), id);
  }

  @PutMapping("/{id}")
  public ProductResponse updateProduct(
      Authentication authentication,
      @PathVariable Long id,
      @Valid @org.springframework.web.bind.annotation.RequestBody ProductUpdateRequest request
  ) {
    return productService.updateProduct(currentUserId(authentication), id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(Authentication authentication, @PathVariable Long id) {
    productService.deleteProduct(currentUserId(authentication), id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/invoice")
  public ResponseEntity<Resource> downloadInvoice(Authentication authentication, @PathVariable Long id) {
    Resource resource = productService.getInvoiceResource(currentUserId(authentication), id);
    String fileName = resource.getFilename() == null ? "invoice" : resource.getFilename();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .body(resource);
  }

  private Long currentUserId(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new UnauthorizedException("Authentication required.");
    }
    return Long.parseLong(authentication.getName());
  }
}
