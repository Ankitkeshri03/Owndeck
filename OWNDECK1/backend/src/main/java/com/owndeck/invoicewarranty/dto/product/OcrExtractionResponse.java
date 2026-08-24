package com.owndeck.invoicewarranty.dto.product;

public class OcrExtractionResponse {

  private String extractedText;
  private ExtractedInvoiceData fields;

  public String getExtractedText() {
    return extractedText;
  }

  public void setExtractedText(String extractedText) {
    this.extractedText = extractedText;
  }

  public ExtractedInvoiceData getFields() {
    return fields;
  }

  public void setFields(ExtractedInvoiceData fields) {
    this.fields = fields;
  }
}
