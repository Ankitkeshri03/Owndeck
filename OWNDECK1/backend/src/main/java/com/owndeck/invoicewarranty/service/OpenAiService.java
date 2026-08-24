package com.owndeck.invoicewarranty.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owndeck.invoicewarranty.dto.ai.ComplaintResponse;
import com.owndeck.invoicewarranty.dto.product.ExtractedInvoiceData;
import com.owndeck.invoicewarranty.entity.ProductInvoice;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiService {

  private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
  private static final List<String> EMAIL_STYLE_VARIATIONS = List.of(
      "Use a concise and direct structure with varied sentence lengths.",
      "Use a warm but firm tone with natural professional wording.",
      "Write with a calm customer-service style and avoid repeating stock phrases.",
      "Use a slightly more formal business tone with clear next-step language."
  );

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final String baseUrl;

  public OpenAiService(
      RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Value("${app.openai.api-key}") String apiKey,
      @Value("${app.openai.model}") String model,
      @Value("${app.openai.base-url}") String baseUrl
  ) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model;
    this.baseUrl = baseUrl;
  }

  public boolean isEnabled() {
    return !apiKey.isBlank();
  }

  public Optional<ExtractedInvoiceData> extractStructuredInvoice(String ocrText) {
    if (!isEnabled() || ocrText == null || ocrText.isBlank()) {
      return Optional.empty();
    }

    String systemPrompt = """
        You extract structured invoice fields from noisy OCR text.
        Return strict JSON only with:
        {
          "productName": "",
          "titleDescription": "",
          "quantity": 1,
          "purchaseDate": "YYYY-MM-DD",
          "warrantyDurationMonths": 0,
          "expiryDate": "YYYY-MM-DD"
        }
        Use null when unknown.
        """;

    String responseText = complete(systemPrompt, ocrText, 0.0);
    if (responseText == null || responseText.isBlank()) {
      return Optional.empty();
    }

    String json = extractJsonObject(responseText);
    if (json == null) {
      return Optional.empty();
    }

    try {
      JsonNode node = objectMapper.readTree(json);
      ExtractedInvoiceData data = new ExtractedInvoiceData();
      data.setProductName(readString(node, "productName"));
      data.setTitleDescription(readString(node, "titleDescription"));
      data.setQuantity(readInteger(node, "quantity"));
      data.setPurchaseDate(readDate(node, "purchaseDate"));
      data.setWarrantyDurationMonths(readInteger(node, "warrantyDurationMonths"));
      data.setExpiryDate(readDate(node, "expiryDate"));
      return Optional.of(data);
    } catch (JsonProcessingException ex) {
      log.warn("Unable to parse OpenAI extraction JSON: {}", ex.getMessage());
      return Optional.empty();
    }
  }

  public ComplaintResponse generateComplaint(ProductInvoice product, String issueDescription) {
    String prompt = normalizeIssuePrompt(issueDescription);
    String fallback = fallbackComplaintBody(issueDescription);
    String fallbackSubject = fallbackComplaintSubject(issueDescription);
    if (!isEnabled()) {
      return new ComplaintResponse(
          fallbackSubject,
          fallback
      );
    }

    String systemPrompt = """
        You are an expert email writer.
        Convert a short, simple customer issue prompt into a complete professional complaint email.
        Return strict JSON only:
        {
          "subject": "",
          "body": ""
        }
        Requirements:
        - Treat the user's input as a short prompt, not as final email wording.
        - Expand it into natural, polished business English.
        - Write like a careful professional customer.
        - Keep the tone respectful, clear, firm, and actionable.
        - The user's issue prompt is the primary source of truth.
        - Stay centered on the exact problem described by the user.
        - Do not add extra issues, dates, warranty claims, or product details unless they are explicitly present in the user prompt.
        - If optional context is unclear, ignore it rather than guessing.
        - Ask for a practical resolution such as replacement, repair, refund, correction, or next steps based on the issue.
        - Do not repeat raw broken input literally if it sounds unnatural.
        - The body should be ready to send as-is.
        - Use noticeably fresh wording and avoid template-like repetition.
        """;

    String userPrompt = """
        Simple issue prompt from user: %s

        Optional product context to use only if directly helpful and consistent with the user's issue:
        Product name: %s
        Product description: %s
        Quantity: %s
        Purchase date: %s
        Warranty expiry date: %s

        Style guidance for this draft: %s

        Desired outcome: Write a full professional complaint email based on the simple issue prompt.
        """.formatted(
        prompt,
        safeProductName(product),
        safeProductDescription(product),
        String.valueOf(product.getQuantity()),
        product.getPurchaseDate() != null ? ISO_DATE.format(product.getPurchaseDate()) : "N/A",
        product.getExpiryDate() != null ? ISO_DATE.format(product.getExpiryDate()) : "N/A",
        pickEmailStyleVariation()
    );

    String responseText = complete(systemPrompt, userPrompt, 0.85);
    if (responseText == null || responseText.isBlank()) {
      return new ComplaintResponse(
          fallbackSubject,
          fallback
      );
    }

    String json = extractJsonObject(responseText);
    if (json == null) {
      return new ComplaintResponse(
          fallbackSubject,
          fallback
      );
    }

    try {
      JsonNode node = objectMapper.readTree(json);
      String subject = readString(node, "subject");
      String body = readString(node, "body");
      if (subject == null || subject.isBlank()) {
        subject = fallbackComplaintSubject(issueDescription);
      }
      if (body == null || body.isBlank()) {
        body = fallback;
      }
      return new ComplaintResponse(subject, body);
    } catch (JsonProcessingException ex) {
      log.warn("Unable to parse OpenAI complaint JSON: {}", ex.getMessage());
      return new ComplaintResponse(
          fallbackSubject,
          fallback
      );
    }
  }

  public GeneratedEmail generateWarrantyAlert(ProductInvoice product, String recipientName, long daysRemaining) {
    String fallbackSubject = fallbackWarrantyAlertSubject(daysRemaining);
    String fallbackBody = fallbackWarrantyAlertBody(product, recipientName, daysRemaining);
    if (!isEnabled()) {
      return new GeneratedEmail(fallbackSubject, fallbackBody);
    }

    String systemPrompt = """
        You write short warranty reminder emails for consumer products.
        Return strict JSON only:
        {
          "subject": "",
          "body": ""
        }
        Requirements:
        - Write clear, human-sounding email copy.
        - Keep the tone helpful and lightly urgent, not robotic.
        - Vary wording from one email to another.
        - Mention the product name and expiry timing naturally.
        - Keep the message compact and ready to send.
        - Do not invent discounts, policies, or promises.
        """;

    String userPrompt = """
        Recipient first name or greeting name: %s
        Product name: %s
        Product description: %s
        Purchase date: %s
        Warranty expiry date: %s
        Days remaining until expiry: %s
        Style guidance for this draft: %s

        Desired outcome: Write a complete reminder email encouraging the user to review the invoice, prepare any claim, and take action before the warranty expires.
        """.formatted(
        normalizeRecipientName(recipientName),
        safeProductName(product),
        safeProductDescription(product),
        product.getPurchaseDate() != null ? ISO_DATE.format(product.getPurchaseDate()) : "N/A",
        product.getExpiryDate() != null ? ISO_DATE.format(product.getExpiryDate()) : "N/A",
        daysRemaining,
        pickEmailStyleVariation()
    );

    String responseText = complete(systemPrompt, userPrompt, 0.95);
    if (responseText == null || responseText.isBlank()) {
      return new GeneratedEmail(fallbackSubject, fallbackBody);
    }

    String json = extractJsonObject(responseText);
    if (json == null) {
      return new GeneratedEmail(fallbackSubject, fallbackBody);
    }

    try {
      JsonNode node = objectMapper.readTree(json);
      String subject = readString(node, "subject");
      String body = readString(node, "body");
      return new GeneratedEmail(
          subject == null || subject.isBlank() ? fallbackSubject : subject,
          body == null || body.isBlank() ? fallbackBody : body
      );
    } catch (JsonProcessingException ex) {
      log.warn("Unable to parse OpenAI warranty alert JSON: {}", ex.getMessage());
      return new GeneratedEmail(fallbackSubject, fallbackBody);
    }
  }

  private String complete(String systemPrompt, String userPrompt, double temperature) {
    try {
      HttpHeaders headers = new HttpHeaders();
      if (!apiKey.isBlank()) {
        headers.setBearerAuth(apiKey);
      }
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> payload = new HashMap<>();
      payload.put("model", model);
      payload.put("temperature", temperature);
      payload.put("messages", List.of(
          Map.of("role", "system", "content", systemPrompt),
          Map.of("role", "user", "content", userPrompt)
      ));

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(
          baseUrl + "/chat/completions",
          request,
          String.class
      );

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        return null;
      }

      JsonNode root = objectMapper.readTree(response.getBody());
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      return content.isMissingNode() ? null : content.asText();
    } catch (JsonProcessingException | RestClientException ex) {
      log.warn("OpenAI call failed, using fallback: {}", ex.getMessage());
      return null;
    }
  }

  private String fallbackComplaintBody(String issueDescription) {
    String issue = professionalizeIssue(issueDescription);
    String requestedResolution = inferResolution(issue);

    return """
        Dear Support Team,

        I am writing to raise a complaint regarding the following issue.

        The issue is as follows:
        %s

        I request %s. Please review this matter and let me know the next steps, any documents you require, and the expected resolution timeline.

        I would appreciate your prompt assistance in resolving this issue at the earliest.

        Sincerely,
        Customer
        """.formatted(
        issue,
        requestedResolution
    );
  }

  private String fallbackComplaintSubject(String issueDescription) {
    String issue = issueDescription == null ? "" : issueDescription.trim().toLowerCase(Locale.ROOT);
    if (issue.contains("not working") || issue.contains("stopped working") || issue.contains("defect")) {
      return "Complaint Regarding Product Malfunction";
    }
    if (issue.contains("damaged") || issue.contains("broken")) {
      return "Complaint Regarding Damaged Product";
    }
    if (issue.contains("different product") || issue.contains("wrong product") || issue.contains("wrong item")) {
      return "Complaint Regarding Incorrect Product Delivered";
    }
    if (issue.contains("missing") || issue.contains("not received")) {
      return "Complaint Regarding Missing Item";
    }
    return "Complaint Regarding Product Issue";
  }

  private String fallbackWarrantyAlertSubject(long daysRemaining) {
    return daysRemaining == 30
        ? "OwnDeck reminder: warranty expires in 30 days"
        : "OwnDeck reminder: warranty expires tomorrow";
  }

  private String fallbackWarrantyAlertBody(ProductInvoice product, String recipientName, long daysRemaining) {
    String greeting = normalizeRecipientName(recipientName);
    String expiryText = daysRemaining == 1 ? "tomorrow" : "in " + daysRemaining + " days";
    return """
        Hello %s,

        This is a quick reminder from OwnDeck that the warranty for %s expires %s.

        Purchase date: %s
        Warranty expiry date: %s

        Please review your invoice and keep any documents ready if you may need a claim or service request.

        Regards,
        OwnDeck
        """.formatted(
        greeting,
        safeProductName(product),
        expiryText,
        product.getPurchaseDate() != null ? ISO_DATE.format(product.getPurchaseDate()) : "N/A",
        product.getExpiryDate() != null ? ISO_DATE.format(product.getExpiryDate()) : "N/A"
    );
  }

  private String inferResolution(String issue) {
    String normalized = issue == null ? "" : issue.toLowerCase(Locale.ROOT);
    if (normalized.contains("different product") || normalized.contains("wrong product") || normalized.contains("wrong item")) {
      return "a replacement with the correct product or another suitable resolution";
    }
    if (normalized.contains("missing") || normalized.contains("not received")) {
      return "delivery of the missing item, a replacement, or another suitable resolution";
    }
    if (normalized.contains("refund")) {
      return "a refund";
    }
    if (normalized.contains("replace") || normalized.contains("replacement")) {
      return "a replacement";
    }
    if (normalized.contains("repair") || normalized.contains("service")) {
      return "a repair or service inspection";
    }
    return "a repair, replacement, or other suitable resolution";
  }

  private String safeProductName(ProductInvoice product) {
    String primary = cleanCatalogText(product.getProductName());
    if (isUsefulProductLabel(primary)) {
      return primary;
    }
    String secondary = cleanCatalogText(product.getTitleDescription());
    if (isUsefulProductLabel(secondary)) {
      return secondary;
    }
    return "the product";
  }

  private String safeProductDescription(ProductInvoice product) {
    String description = cleanCatalogText(product.getTitleDescription());
    if (description.isBlank() || description.equalsIgnoreCase(safeProductName(product))) {
      return "N/A";
    }
    return description;
  }

  private String professionalizeIssue(String issueDescription) {
    String raw = issueDescription == null ? "" : issueDescription.trim();
    if (raw.isBlank()) {
      return "The product is malfunctioning during normal use and requires warranty resolution.";
    }

    String normalized = raw.toLowerCase(Locale.ROOT);
    if (normalized.contains("different product") || normalized.contains("wrong product") || normalized.contains("wrong item")) {
      return "I received a different product from the one that was ordered or expected.";
    }
    if (normalized.contains("missing")) {
      return "The delivered order is incomplete, and one or more expected items are missing.";
    }
    if (normalized.contains("not working") || normalized.contains("stopped working")) {
      return "The product is not functioning properly during normal use.";
    }
    if (normalized.contains("damaged") || normalized.contains("broken")) {
      return "The product was delivered in damaged condition or has developed physical damage during normal use.";
    }
    if (normalized.contains("defect")) {
      return "The product appears to have a manufacturing defect that is affecting its normal use.";
    }

    String sentence = raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
    if (!sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?")) {
      sentence = sentence + ".";
    }
    return sentence;
  }

  private String normalizeIssuePrompt(String issueDescription) {
    String raw = issueDescription == null ? "" : issueDescription.trim();
    return raw.isBlank() ? "The product has an issue and I need support." : raw;
  }

  private String normalizeRecipientName(String recipientName) {
    String raw = recipientName == null ? "" : recipientName.trim();
    return raw.isBlank() ? "there" : raw;
  }

  private String cleanCatalogText(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replaceAll("(?i)e\\.?\\s*&\\s*o\\.?\\s*e\\.?", " ")
        .replaceAll("(?i)page\\s*\\d+\\s*of\\s*\\d+", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private boolean isUsefulProductLabel(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String normalized = value.trim();
    if (normalized.length() < 3 || normalized.length() > 120) {
      return false;
    }
    String lower = normalized.toLowerCase(Locale.ROOT);
    if (lower.matches(".*\\b(page|invoice|bill|qty|quantity|date)\\b.*")) {
      return false;
    }
    long letters = normalized.chars().filter(Character::isLetter).count();
    return letters >= 3;
  }

  private String pickEmailStyleVariation() {
    int index = ThreadLocalRandom.current().nextInt(EMAIL_STYLE_VARIATIONS.size());
    return EMAIL_STYLE_VARIATIONS.get(index);
  }

  private String extractJsonObject(String text) {
    int start = text.indexOf("{");
    int end = text.lastIndexOf("}");
    if (start < 0 || end < 0 || end <= start) {
      return null;
    }
    return text.substring(start, end + 1);
  }

  private String readString(JsonNode node, String key) {
    JsonNode value = node.path(key);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String s = value.asText(null);
    return s == null || s.isBlank() ? null : s.trim();
  }

  private Integer readInteger(JsonNode node, String key) {
    JsonNode value = node.path(key);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.isNumber() ? value.asInt() : null;
  }

  private java.time.LocalDate readDate(JsonNode node, String key) {
    String raw = readString(node, key);
    if (raw == null) {
      return null;
    }
    try {
      return java.time.LocalDate.parse(raw, ISO_DATE);
    } catch (Exception ex) {
      return null;
    }
  }

  public record GeneratedEmail(String subject, String body) {
  }

}
