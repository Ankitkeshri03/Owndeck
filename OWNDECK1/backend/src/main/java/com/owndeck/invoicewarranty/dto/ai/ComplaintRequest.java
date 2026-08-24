package com.owndeck.invoicewarranty.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
    @NotBlank(message = "Issue description is required")
    @Size(max = 1200, message = "Issue description is too long")
    String issueDescription
) {
}
