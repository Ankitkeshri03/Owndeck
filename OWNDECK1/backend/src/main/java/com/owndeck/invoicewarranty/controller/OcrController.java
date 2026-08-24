package com.owndeck.invoicewarranty.controller;

import com.owndeck.invoicewarranty.dto.product.ExtractedInvoiceData;
import com.owndeck.invoicewarranty.dto.product.OcrExtractionResponse;
import com.owndeck.invoicewarranty.service.FileStorageService;
import com.owndeck.invoicewarranty.service.InvoiceExtractionService;
import com.owndeck.invoicewarranty.service.OcrService;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

  private final FileStorageService fileStorageService;
  private final OcrService ocrService;
  private final InvoiceExtractionService extractionService;

  public OcrController(
      FileStorageService fileStorageService,
      OcrService ocrService,
      InvoiceExtractionService extractionService
  ) {
    this.fileStorageService = fileStorageService;
    this.ocrService = ocrService;
    this.extractionService = extractionService;
  }

  @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public OcrExtractionResponse extract(@RequestParam("file") MultipartFile file) {
    FileStorageService.StoredFile stored = fileStorageService.storeInvoice(file);
    String text = ocrService.extractText(stored.path(), stored.contentType());
    ExtractedInvoiceData data = extractionService.extract(text);

    OcrExtractionResponse response = new OcrExtractionResponse();
    response.setExtractedText(text);
    response.setFields(data);

    try {
      Files.deleteIfExists(stored.path());
    } catch (IOException ignored) {
      // No-op cleanup.
    }

    return response;
  }
}
