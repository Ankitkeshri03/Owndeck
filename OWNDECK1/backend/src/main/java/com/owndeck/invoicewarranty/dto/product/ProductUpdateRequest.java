package com.owndeck.invoicewarranty.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProductUpdateRequest(
    @Size(max = 255, message = "Product name is too long")
    String productName,
    @Size(max = 1200, message = "Title/description is too long")
    String titleDescription,
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10000, message = "Quantity is too large")
    Integer quantity,
    LocalDate purchaseDate,
    @Min(value = 0, message = "Warranty months cannot be negative")
    @Max(value = 240, message = "Warranty months is too large")
    Integer warrantyDurationMonths,
    LocalDate expiryDate
) {
}
