package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.dto.product.ExtractedInvoiceData;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class InvoiceExtractionService {

  private static final Pattern PRODUCT_PATTERN = Pattern.compile(
      "(?i)(?:product\\s*name|item\\s*name|item|product|model(?:\\s*name)?|description|particulars?)\\s*[:\\-]\\s*(.+)"
  );
  private static final Pattern QTY_PATTERN = Pattern.compile(
      "(?i)(?:qty|quantity)\\s*[:\\-]?\\s*(\\d+)"
  );
  private static final Pattern PURCHASE_DATE_LABEL_PATTERN = Pattern.compile(
      "(?i)(?:purchase\\s*date|invoice\\s*date|date\\s*of\\s*invoice|bill\\s*date|dated)\\s*[:\\-]?\\s*(.+)"
  );
  private static final Pattern EXPIRY_LABEL_PATTERN = Pattern.compile(
      "(?i)(?:expiry\\s*date|warranty\\s*till|valid\\s*till|expires\\s*on|warranty\\s*expiry)\\s*[:\\-]?\\s*(.+)"
  );
  private static final Pattern NON_PURCHASE_DATE_LABEL_PATTERN = Pattern.compile(
      "(?i)(?:expiry|expires|valid\\s*till|due\\s*date|delivery\\s*date|ship(?:ped|ping)?\\s*date|order\\s*date)"
  );
  private static final Pattern WARRANTY_PATTERN = Pattern.compile(
      "(?i)(?:(?:warranty|guarantee)(?:\\s*period)?\\s*[:\\-]?\\s*(\\d{1,3})\\s*(month|months|year|years)|"
          + "(\\d{1,3})\\s*(month|months|year|years)\\s*(?:manufacturer\\s*)?(?:warranty|guarantee))"
  );
  private static final Pattern DATE_CANDIDATE_PATTERN = Pattern.compile(
      "(?i)\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{2,4}|[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{2,4})\\b"
  );

  private static final List<DateTimeFormatter> TEXTUAL_DATE_FORMATS = List.of(
      formatter("d MMM uu"),
      formatter("d MMMM uu"),
      formatter("d MMM uuuu"),
      formatter("d MMMM uuuu"),
      formatter("MMM d uu"),
      formatter("MMMM d uu"),
      formatter("MMM d uuuu"),
      formatter("MMMM d uuuu")
  );

  private final OpenAiService openAiService;

  public InvoiceExtractionService(OpenAiService openAiService) {
    this.openAiService = openAiService;
  }

  public ExtractedInvoiceData extract(String rawText) {
    ExtractedInvoiceData base = extractWithRegex(rawText);
    Optional<ExtractedInvoiceData> ai = openAiService.extractStructuredInvoice(rawText);
    if (ai.isEmpty()) {
      return base;
    }
    return merge(base, ai.get());
  }

  private ExtractedInvoiceData extractWithRegex(String rawText) {
    String safeText = rawText == null ? "" : rawText;
    ExtractedInvoiceData data = new ExtractedInvoiceData();

    String productName = firstGroup(PRODUCT_PATTERN, safeText);
    if (productName == null) {
      productName = pickFallbackProductName(safeText);
    }
    data.setProductName(productName);
    data.setTitleDescription(productName);
    data.setQuantity(parseInteger(firstGroup(QTY_PATTERN, safeText), 1));
    data.setPurchaseDate(findPurchaseDate(safeText));

    Integer warrantyMonths = null;
    Matcher warrantyMatcher = WARRANTY_PATTERN.matcher(safeText);
    if (warrantyMatcher.find()) {
      String rawValue = firstNonBlank(warrantyMatcher.group(1), warrantyMatcher.group(3));
      String rawUnit = firstNonBlank(warrantyMatcher.group(2), warrantyMatcher.group(4));
      int value = parseInteger(rawValue, 0);
      String unit = rawUnit == null ? "" : rawUnit.toLowerCase(Locale.ROOT);
      warrantyMonths = unit.startsWith("year") ? value * 12 : value;
    }
    data.setWarrantyDurationMonths(warrantyMonths);

    LocalDate expiryDate = findLabeledDate(safeText, EXPIRY_LABEL_PATTERN);
    if (expiryDate == null && data.getPurchaseDate() != null && warrantyMonths != null) {
      expiryDate = data.getPurchaseDate().plusMonths(warrantyMonths);
    }
    data.setExpiryDate(expiryDate);
    return data;
  }

  private ExtractedInvoiceData merge(ExtractedInvoiceData regex, ExtractedInvoiceData ai) {
    ExtractedInvoiceData merged = new ExtractedInvoiceData();
    merged.setProductName(firstNonBlank(ai.getProductName(), regex.getProductName()));
    merged.setTitleDescription(firstNonBlank(ai.getTitleDescription(), regex.getTitleDescription()));
    merged.setQuantity(firstNonNull(ai.getQuantity(), regex.getQuantity(), 1));
    merged.setPurchaseDate(firstNonNull(regex.getPurchaseDate(), ai.getPurchaseDate(), null));
    merged.setWarrantyDurationMonths(firstNonNull(
        ai.getWarrantyDurationMonths(),
        regex.getWarrantyDurationMonths(),
        0
    ));
    LocalDate expiry = firstNonNull(regex.getExpiryDate(), ai.getExpiryDate(), null);
    if (expiry == null && merged.getPurchaseDate() != null) {
      expiry = merged.getPurchaseDate().plusMonths(merged.getWarrantyDurationMonths());
    }
    merged.setExpiryDate(expiry);
    return merged;
  }

  private String firstGroup(Pattern pattern, String value) {
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private String pickFallbackProductName(String rawText) {
    String bestCandidate = null;
    int bestScore = Integer.MIN_VALUE;
    String[] lines = rawText.split("\\R");
    for (int index = 0; index < lines.length; index++) {
      String cleaned = sanitizeCandidateLine(lines[index]);
      if (cleaned.isBlank() || cleaned.length() < 4 || cleaned.length() > 140) {
        continue;
      }

      String lower = cleaned.toLowerCase(Locale.ROOT);
      if (lower.matches(".*\\b(invoice|bill|gst|total|amount|date|qty|quantity|warranty|tax)\\b.*")) {
        continue;
      }

      int score = 0;
      if (containsMixedModelAndWords(cleaned)) {
        score += 8;
      }
      if (cleaned.length() >= 8 && cleaned.length() <= 80) {
        score += 4;
      }
      score += Math.max(0, 6 - index);

      long letterCount = cleaned.chars().filter(Character::isLetter).count();
      if (letterCount >= 4) {
        score += 3;
      }

      if (score > bestScore) {
        bestScore = score;
        bestCandidate = cleaned;
      }
    }
    return bestCandidate;
  }

  private Integer parseInteger(String raw, int fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private LocalDate findLabeledDate(String rawText, Pattern labelPattern) {
    for (String line : rawText.split("\\R")) {
      Matcher matcher = labelPattern.matcher(line);
      if (!matcher.find()) {
        continue;
      }

      LocalDate candidate = extractDateCandidate(matcher.group(1));
      if (candidate != null) {
        return candidate;
      }

      candidate = extractDateCandidate(line);
      if (candidate != null) {
        return candidate;
      }
    }
    return null;
  }

  private LocalDate findPurchaseDate(String rawText) {
    LocalDate labeled = findLabeledDate(rawText, PURCHASE_DATE_LABEL_PATTERN);
    if (labeled != null) {
      return labeled;
    }

    List<DatedCandidate> candidates = new ArrayList<>();
    String[] lines = rawText.split("\\R");
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      boolean looksLikeNonPurchaseLine = NON_PURCHASE_DATE_LABEL_PATTERN.matcher(line).find();
      Matcher matcher = DATE_CANDIDATE_PATTERN.matcher(normalizeDateText(line));
      while (matcher.find()) {
        LocalDate date = parseDate(matcher.group());
        if (date == null || date.isAfter(LocalDate.now().plusDays(2))) {
          continue;
        }

        int score = looksLikeNonPurchaseLine ? -5 : 0;
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("invoice") || lower.contains("purchase") || lower.contains("bill") || lower.contains("dated")) {
          score += 5;
        }
        if (lower.contains("date")) {
          score += 2;
        }
        score += Math.max(0, 10 - index);
        candidates.add(new DatedCandidate(date, score));
      }
    }

    return candidates.stream()
        .max(Comparator.comparingInt(DatedCandidate::score)
            .thenComparing(DatedCandidate::date, Comparator.reverseOrder()))
        .map(DatedCandidate::date)
        .orElse(null);
  }

  private LocalDate extractDateCandidate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }

    Matcher matcher = DATE_CANDIDATE_PATTERN.matcher(normalizeDateText(raw));
    while (matcher.find()) {
      LocalDate parsed = parseDate(matcher.group());
      if (parsed != null) {
        return parsed;
      }
    }
    return parseDate(raw);
  }

  private LocalDate parseDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }

    String cleaned = normalizeDateText(raw)
        .replace(",", " ")
        .replace("'", " ")
        .replaceAll("\\s+", " ")
        .replaceAll("(?<=\\d)(st|nd|rd|th)\\b", "");

    LocalDate iso = tryParse(cleaned, DateTimeFormatter.ISO_DATE);
    if (iso != null && isReasonableYear(iso.getYear())) {
      return iso;
    }

    LocalDate slashOrDash = parseNumericDate(cleaned);
    if (slashOrDash != null) {
      return slashOrDash;
    }

    for (DateTimeFormatter formatter : TEXTUAL_DATE_FORMATS) {
      LocalDate parsed = tryParse(cleaned, formatter);
      if (parsed != null && isReasonableYear(parsed.getYear())) {
        return parsed;
      }
    }
    return null;
  }

  private LocalDate parseNumericDate(String raw) {
    String normalized = raw.replace('.', '/').replace('-', '/');
    String[] parts = normalized.split("/");
    if (parts.length != 3) {
      return null;
    }

    Integer first = safeParseNumber(parts[0]);
    Integer second = safeParseNumber(parts[1]);
    Integer third = safeParseNumber(parts[2]);
    if (first == null || second == null || third == null) {
      return null;
    }

    if (parts[0].trim().length() == 4) {
      int year = normalizeYear(first);
      if (!isReasonableYear(year)) {
        return null;
      }
      return buildDate(year, second, third);
    }

    int year = normalizeYear(third);
    if (!isReasonableYear(year)) {
      return null;
    }

    LocalDate ddMm = buildDate(year, second, first);
    LocalDate mmDd = buildDate(year, first, second);

    if (first > 12 && ddMm != null) {
      return ddMm;
    }
    if (second > 12 && mmDd != null) {
      return mmDd;
    }
    if (ddMm != null) {
      return ddMm;
    }
    return mmDd;
  }

  private LocalDate buildDate(int year, int month, int day) {
    try {
      return LocalDate.of(year, month, day);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private LocalDate tryParse(String raw, DateTimeFormatter formatter) {
    try {
      return LocalDate.parse(raw, formatter);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private Integer safeParseNumber(String raw) {
    try {
      return Integer.parseInt(normalizeNumericToken(raw));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private int normalizeYear(int year) {
    if (year >= 100) {
      return year;
    }
    return year >= 70 ? 1900 + year : 2000 + year;
  }

  private boolean isReasonableYear(int year) {
    int currentYear = Year.now().getValue();
    return year >= 1990 && year <= currentYear + 2;
  }

  private String sanitizeCandidateLine(String line) {
    if (line == null) {
      return "";
    }
    return line
        .replaceAll("[|]+", " ")
        .replaceAll("\\s{2,}", " ")
        .replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "")
        .trim();
  }

  private boolean containsMixedModelAndWords(String value) {
    boolean hasLetters = value.chars().anyMatch(Character::isLetter);
    boolean hasDigits = value.chars().anyMatch(Character::isDigit);
    return hasLetters && hasDigits;
  }

  private static DateTimeFormatter formatter(String pattern) {
    return new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern(pattern)
        .toFormatter(Locale.ENGLISH)
        .withResolverStyle(ResolverStyle.STRICT);
  }

  private String normalizeDateText(String raw) {
    String normalized = raw.trim()
        .replaceAll("(?<=\\d)\\s*([/\\-.])\\s*(?=\\d)", "$1")
        .replaceAll("(?<=\\d)[Oo](?=\\d)", "0")
        .replaceAll("(?<=\\d)[Il](?=\\d)", "1")
        .replaceAll("(?<=\\d)[Ss](?=\\d)", "5")
        .replaceAll("(?<=\\d)[Bb](?=\\d)", "8");
    return normalized;
  }

  private String normalizeNumericToken(String raw) {
    return raw.trim()
        .replace('O', '0')
        .replace('o', '0')
        .replace('I', '1')
        .replace('l', '1')
        .replace('S', '5')
        .replace('s', '5')
        .replace('B', '8');
  }

  private record DatedCandidate(LocalDate date, int score) {
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first.trim();
    }
    return second;
  }

  @SafeVarargs
  private <T> T firstNonNull(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
