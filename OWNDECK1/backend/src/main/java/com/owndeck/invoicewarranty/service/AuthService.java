package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.dto.auth.AuthResponse;
import com.owndeck.invoicewarranty.dto.auth.LoginRequest;
import com.owndeck.invoicewarranty.dto.auth.SignupRequest;
import com.owndeck.invoicewarranty.dto.auth.UpdateAlertEmailRequest;
import com.owndeck.invoicewarranty.dto.auth.UserResponse;
import com.owndeck.invoicewarranty.entity.User;
import com.owndeck.invoicewarranty.exception.BadRequestException;
import com.owndeck.invoicewarranty.exception.NotFoundException;
import com.owndeck.invoicewarranty.exception.UnauthorizedException;
import com.owndeck.invoicewarranty.repository.UserRepository;
import com.owndeck.invoicewarranty.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResponse signup(SignupRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new BadRequestException("Email is already registered.");
    }

    User user = new User();
    user.setName(request.name().trim());
    user.setEmail(email);
    user.setPhone(request.phone() == null ? "" : request.phone().trim());
    user.setAlertEmail(resolveAlertEmail(request.alertEmail(), email));
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    User saved = userRepository.save(user);

    String token = jwtService.generateToken(saved.getId());
    return new AuthResponse(token, toUserResponse(saved));
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    User user = userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid email or password.");
    }

    String token = jwtService.generateToken(user.getId());
    return new AuthResponse(token, toUserResponse(user));
  }

  @Transactional(readOnly = true)
  public UserResponse me(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found."));
    return toUserResponse(user);
  }

  @Transactional
  public UserResponse updateAlertEmail(Long userId, UpdateAlertEmailRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found."));
    user.setAlertEmail(resolveAlertEmail(request.alertEmail(), user.getEmail()));
    User saved = userRepository.save(user);
    return toUserResponse(saved);
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getPhone(),
        resolveAlertEmail(user.getAlertEmail(), user.getEmail())
    );
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }

  private String resolveAlertEmail(String alertEmail, String fallbackEmail) {
    String normalized = normalizeEmail(alertEmail);
    if (!normalized.isBlank()) {
      return normalized;
    }
    return normalizeEmail(fallbackEmail);
  }
}
