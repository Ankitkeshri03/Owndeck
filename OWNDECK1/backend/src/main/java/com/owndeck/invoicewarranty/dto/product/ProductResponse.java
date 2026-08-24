package com.owndeck.invoicewarranty.dto.product;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductResponse {

  private Long id;
  private Long userId;
  private String productName;
  private String titleDescription;
  private Integer quantity;
  private LocalDate purchaseDate;
  private LocalDate expiryDate;
  private Integer warrantyDurationMonths;
  private String invoiceUrl;
  private String status;
  private Integer daysRemaining;
  private String rawOcrText;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

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

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public Integer getWarrantyDurationMonths() {
    return warrantyDurationMonths;
  }

  public void setWarrantyDurationMonths(Integer warrantyDurationMonths) {
    this.warrantyDurationMonths = warrantyDurationMonths;
  }

  public String getInvoiceUrl() {
    return invoiceUrl;
  }

  public void setInvoiceUrl(String invoiceUrl) {
    this.invoiceUrl = invoiceUrl;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getDaysRemaining() {
    return daysRemaining;
  }

  public void setDaysRemaining(Integer daysRemaining) {
    this.daysRemaining = daysRemaining;
  }

  public String getRawOcrText() {
    return rawOcrText;
  }

  public void setRawOcrText(String rawOcrText) {
    this.rawOcrText = rawOcrText;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
