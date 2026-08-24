package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.entity.ProductInvoice;
import com.owndeck.invoicewarranty.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AlertEmailService {

  private static final Logger log = LoggerFactory.getLogger(AlertEmailService.class);

  private final JavaMailSender mailSender;
  private final boolean enabled;
  private final String fromAddress;
  private final OpenAiService openAiService;

  public AlertEmailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      OpenAiService openAiService,
      @Value("${app.alert.email.enabled:false}") boolean enabled,
      @Value("${app.alert.email.from-address:no-reply@owndeck.local}") String fromAddress
  ) {
    this.mailSender = mailSenderProvider.getIfAvailable();
    this.openAiService = openAiService;
    this.enabled = enabled;
    this.fromAddress = fromAddress;
  }

  public void sendWarrantyAlert(User user, ProductInvoice product, long daysRemaining) {
    String targetEmail = resolveTargetEmail(user);
    if (targetEmail.isBlank()) {
      return;
    }
    if (!enabled || mailSender == null) {
      log.info("Alert email skipped for {} because email delivery is disabled.", targetEmail);
      return;
    }

    OpenAiService.GeneratedEmail generated = openAiService.generateWarrantyAlert(
        product,
        user == null ? null : user.getName(),
        daysRemaining
    );

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(targetEmail);
    message.setSubject(generated.subject());
    message.setText(generated.body());
    mailSender.send(message);
  }

  private String resolveTargetEmail(User user) {
    if (user == null) {
      return "";
    }
    if (user.getAlertEmail() != null && !user.getAlertEmail().isBlank()) {
      return user.getAlertEmail().trim().toLowerCase();
    }
    if (user.getEmail() != null && !user.getEmail().isBlank()) {
      return user.getEmail().trim().toLowerCase();
    }
    return "";
  }
}
