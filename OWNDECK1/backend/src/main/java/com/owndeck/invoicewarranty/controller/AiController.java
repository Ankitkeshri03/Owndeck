package com.owndeck.invoicewarranty.controller;

import com.owndeck.invoicewarranty.dto.ai.ComplaintRequest;
import com.owndeck.invoicewarranty.dto.ai.ComplaintResponse;
import com.owndeck.invoicewarranty.exception.UnauthorizedException;
import com.owndeck.invoicewarranty.service.ComplaintService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
  private static final Logger log = LoggerFactory.getLogger(AiController.class);

  private final ComplaintService complaintService;

  public AiController(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  @PostMapping("/products/{id}/complaint")
  public ComplaintResponse generateComplaint(
      Authentication authentication,
      @PathVariable Long id,
      @Valid @RequestBody ComplaintRequest request
  ) {
    try {
      return complaintService.generateComplaint(currentUserId(authentication), id, request.issueDescription());
    } catch (Exception ex) {
      log.error("Complaint email generation failed for product {}", id, ex);
      throw ex;
    }
  }

  private Long currentUserId(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new UnauthorizedException("Authentication required.");
    }
    return Long.parseLong(authentication.getName());
  }
}
