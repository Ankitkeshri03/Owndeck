package com.owndeck.invoicewarranty.dto.auth;

public record UserResponse(
    Long id,
    String name,
    String email,
    String phone,
    String alertEmail
) {
}
