package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.exception.BadRequestException;
import com.owndeck.invoicewarranty.util.ProjectPathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  private static final Set<String> SUPPORTED_MIME = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp",
      "application/pdf"
  );

  private final Path rootLocation;
  private final String publicBaseUrl;

  public FileStorageService(
      @Value("${app.storage.location}") String storageLocation,
      @Value("${app.storage.public-base-url}") String publicBaseUrl
  ) throws IOException {
    this.rootLocation = ProjectPathResolver.resolveBackendPath(storageLocation);
    this.publicBaseUrl = publicBaseUrl;
    Files.createDirectories(this.rootLocation);
  }

  public StoredFile storeInvoice(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Invoice file is required.");
    }
    if (file.getSize() > 10L * 1024 * 1024) {
      throw new BadRequestException("File is too large. Maximum allowed size is 10MB.");
    }
    if (file.getContentType() != null && !SUPPORTED_MIME.contains(file.getContentType())) {
      throw new BadRequestException("Only JPG, PNG, WEBP and PDF files are allowed.");
    }

    String extension = getExtension(file.getOriginalFilename());
    String fileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
    Path targetPath = rootLocation.resolve(fileName).normalize();
    if (!targetPath.startsWith(rootLocation)) {
      throw new BadRequestException("Invalid file path.");
    }

    try {
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new BadRequestException("Unable to store uploaded file.");
    }

    String url = publicBaseUrl + "/uploads/" + fileName;
    return new StoredFile(fileName, targetPath, url, file.getContentType());
  }

  public Resource loadAsResource(String absolutePath) {
    try {
      Path path = Paths.get(absolutePath).toAbsolutePath().normalize();
      Resource resource = new UrlResource(path.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new BadRequestException("Invoice file not found.");
      }
      return resource;
    } catch (IOException ex) {
      throw new BadRequestException("Unable to read invoice file.");
    }
  }

  private String getExtension(String fileName) {
    if (fileName == null) {
      return "";
    }
    int idx = fileName.lastIndexOf(".");
    if (idx < 0 || idx == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(idx + 1).toLowerCase();
  }

  public record StoredFile(String fileName, Path path, String url, String contentType) {
  }
}
