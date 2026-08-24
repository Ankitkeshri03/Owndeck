package com.owndeck.invoicewarranty.dto.auth;

public record AuthResponse(
    String token,
    UserResponse user
) {
}
