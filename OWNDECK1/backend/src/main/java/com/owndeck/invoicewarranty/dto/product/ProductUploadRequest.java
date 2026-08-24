package com.owndeck.invoicewarranty.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ProductUploadRequest {

  @Size(max = 255, message = "Product name is too long")
  private String productName;

  @Size(max = 1200, message = "Title/description is too long")
  private String titleDescription;

  @Min(value = 1, message = "Quantity must be at least 1")
  @Max(value = 10000, message = "Quantity is too large")
  private Integer quantity;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate purchaseDate;

  @Min(value = 0, message = "Warranty months cannot be negative")
  @Max(value = 240, message = "Warranty months is too large")
  private Integer warrantyDurationMonths;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
