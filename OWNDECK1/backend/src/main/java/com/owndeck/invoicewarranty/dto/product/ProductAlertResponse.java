package com.owndeck.invoicewarranty.dto.product;

import java.time.LocalDate;

public record ProductAlertResponse(
    Long productId,
    String productName,
    LocalDate expiryDate,
    Integer daysRemaining,
    String alertType,
    String message
) {
}
