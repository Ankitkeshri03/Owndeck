package com.owndeck.invoicewarranty.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAlertEmailRequest(
    @NotBlank(message = "Alert email is required")
    @Email(message = "Alert email must be valid")
    @Size(max = 160, message = "Alert email is too long")
    String alertEmail
) {
}
