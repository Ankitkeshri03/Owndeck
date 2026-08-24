package com.owndeck.invoicewarranty.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name is too long")
    String name,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
    String password,
    @Size(max = 30, message = "Phone is too long")
    String phone,
    @Email(message = "Alert email must be valid")
    @Size(max = 160, message = "Alert email is too long")
    String alertEmail
) {
}
