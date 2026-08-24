package com.owndeck.invoicewarranty.controller;

import com.owndeck.invoicewarranty.dto.auth.AuthResponse;
import com.owndeck.invoicewarranty.dto.auth.LoginRequest;
import com.owndeck.invoicewarranty.dto.auth.SignupRequest;
import com.owndeck.invoicewarranty.dto.auth.UpdateAlertEmailRequest;
import com.owndeck.invoicewarranty.dto.auth.UserResponse;
import com.owndeck.invoicewarranty.exception.UnauthorizedException;
import com.owndeck.invoicewarranty.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
    return authService.signup(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public UserResponse me(Authentication authentication) {
    return authService.me(currentUserId(authentication));
  }

  @PutMapping("/alert-email")
  public UserResponse updateAlertEmail(
      Authentication authentication,
      @Valid @RequestBody UpdateAlertEmailRequest request
  ) {
    return authService.updateAlertEmail(currentUserId(authentication), request);
  }

  private Long currentUserId(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new UnauthorizedException("Authentication required.");
    }
    return Long.parseLong(authentication.getName());
  }
}
