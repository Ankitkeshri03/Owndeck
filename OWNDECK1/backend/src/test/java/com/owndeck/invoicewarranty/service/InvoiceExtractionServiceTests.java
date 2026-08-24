package com.owndeck.invoicewarranty.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owndeck.invoicewarranty.dto.product.ExtractedInvoiceData;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceExtractionServiceTests {

  @Test
  void extractsOriginalPurchaseDateFromDayMonthYearInvoiceText() {
    InvoiceExtractionService service = new InvoiceExtractionService(disabledOpenAi());

    ExtractedInvoiceData data = service.extract("""
        Tax Invoice
        Product Name: Samsung Galaxy Buds
        Invoice Date: 17/08/2023
        Warranty: 12 months
        """);

    assertEquals(LocalDate.of(2023, 8, 17), data.getPurchaseDate());
    assertEquals(LocalDate.of(2024, 8, 17), data.getExpiryDate());
  }

  @Test
  void keepsRegexPurchaseYearWhenAiSuggestsWrongYear() {
    OpenAiService ai = new FakeOpenAiService(Optional.of(extractedWithPurchaseDate(LocalDate.of(2024, 8, 17))));
    InvoiceExtractionService service = new InvoiceExtractionService(ai);

    ExtractedInvoiceData data = service.extract("""
        Invoice Date: 17/08/2023
        Product Name: Noise Smart Watch
        Warranty: 24 months
        """);

    assertEquals(LocalDate.of(2023, 8, 17), data.getPurchaseDate());
    assertEquals(LocalDate.of(2025, 8, 17), data.getExpiryDate());
  }

  @Test
  void extractsTextualMonthDatesFromInvoice() {
    InvoiceExtractionService service = new InvoiceExtractionService(disabledOpenAi());

    ExtractedInvoiceData data = service.extract("""
        Bill Date: 5 September 2022
        Product Name: HP Laptop
        Warranty: 1 year
        """);

    assertEquals(LocalDate.of(2022, 9, 5), data.getPurchaseDate());
  }

  @Test
  void extractsInlineWarrantyAndFallbackProductFromNoisyText() {
    InvoiceExtractionService service = new InvoiceExtractionService(disabledOpenAi());

    ExtractedInvoiceData data = service.extract("""
        TAX INVOICE
        Noise ColorFit Pro 4 Max
        24 months manufacturer warranty
        Dated: 03-11-2024
        """);

    assertEquals("Noise ColorFit Pro 4 Max", data.getProductName());
    assertEquals(LocalDate.of(2024, 11, 3), data.getPurchaseDate());
    assertEquals(LocalDate.of(2026, 11, 3), data.getExpiryDate());
  }

  @Test
  void extractsYearFirstPdfStyleDateExactly() {
    InvoiceExtractionService service = new InvoiceExtractionService(disabledOpenAi());

    ExtractedInvoiceData data = service.extract("""
        TAX INVOICE
        Invoice Date: 2023/08/17
        Product Name: Boat Headphones
        Warranty: 12 months
        """);

    assertEquals(LocalDate.of(2023, 8, 17), data.getPurchaseDate());
    assertEquals(LocalDate.of(2024, 8, 17), data.getExpiryDate());
  }

  @Test
  void extractsTextualDateWithShortQuotedYear() {
    InvoiceExtractionService service = new InvoiceExtractionService(disabledOpenAi());

    ExtractedInvoiceData data = service.extract("""
        Bill Date: 17 Aug '23
        Product Name: JBL Speaker
        Warranty: 1 year
        """);

    assertEquals(LocalDate.of(2023, 8, 17), data.getPurchaseDate());
  }

  private OpenAiService disabledOpenAi() {
    return new FakeOpenAiService(Optional.empty());
  }

  private ExtractedInvoiceData extractedWithPurchaseDate(LocalDate purchaseDate) {
    ExtractedInvoiceData data = new ExtractedInvoiceData();
    data.setPurchaseDate(purchaseDate);
    return data;
  }

  private static final class FakeOpenAiService extends OpenAiService {
    private final Optional<ExtractedInvoiceData> response;

    private FakeOpenAiService(Optional<ExtractedInvoiceData> response) {
      super(null, new ObjectMapper(), "", "gpt-4o-mini", "https://api.openai.com/v1");
      this.response = response;
    }

    @Override
    public Optional<ExtractedInvoiceData> extractStructuredInvoice(String ocrText) {
      return response;
    }
  }
}
