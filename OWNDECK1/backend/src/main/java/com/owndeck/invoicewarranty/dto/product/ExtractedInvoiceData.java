package com.owndeck.invoicewarranty.dto.product;

import java.time.LocalDate;

public class ExtractedInvoiceData {

  private String productName;
  private String titleDescription;
  private Integer quantity;
  private LocalDate purchaseDate;
  private Integer warrantyDurationMonths;
  private LocalDate expiryDate;

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getTitleDescription() {
    return titleDescription;
  }

  public void setTitleDescription(String titleDescription) {
    this.titleDescription = titleDescription;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public LocalDate getPurchaseDate() {
    return purchaseDate;
  }

  public void setPurchaseDate(LocalDate purchaseDate) {
    this.purchaseDate = purchaseDate;
  }

  public Integer getWarrantyDurationMonths() {
    return warrantyDurationMonths;
  }

  public void setWarrantyDurationMonths(Integer warrantyDurationMonths) {
    this.warrantyDurationMonths = warrantyDurationMonths;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }
}
