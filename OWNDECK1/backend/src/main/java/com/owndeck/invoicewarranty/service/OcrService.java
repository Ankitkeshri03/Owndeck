package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.util.ProjectPathResolver;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OcrService {

  private static final Logger log = LoggerFactory.getLogger(OcrService.class);
  private static final int MIN_PDF_NATIVE_TEXT_LENGTH = 40;
  private static final int MIN_USEFUL_TEXT_LETTERS = 16;

  // Tess4J loads libtesseract through JNA, which only searches the OS default paths.
  // Homebrew on Apple Silicon installs to /opt/homebrew/lib, which is not among them, so
  // without this the native load fails and every OCR call silently returns empty text.
  private static final List<String> DEFAULT_NATIVE_LIB_PATHS =
      List.of("/opt/homebrew/lib", "/usr/local/lib", "/usr/lib", "/usr/lib/x86_64-linux-gnu");

  private final String tessdataPath;

  public OcrService(
      @Value("${app.ocr.tessdata-path}") String tessdataPath,
      @Value("${app.ocr.native-lib-path:}") String configuredNativeLibPath) {
    this.tessdataPath = ProjectPathResolver.resolveBackendPath(tessdataPath).toString();
    registerNativeLibraryPath(configuredNativeLibPath);
  }

  /**
   * Adds the directories that may hold libtesseract to {@code jna.library.path}.
   *
   * <p>JNA reads this property when it first loads the library, which happens lazily on the
   * first OCR call -- so setting it here, at bean construction, is early enough.
   */
  private static void registerNativeLibraryPath(String configuredNativeLibPath) {
    List<String> candidates = new ArrayList<>();
    if (configuredNativeLibPath != null && !configuredNativeLibPath.isBlank()) {
      for (String entry : configuredNativeLibPath.split(File.pathSeparator)) {
        if (!entry.isBlank()) {
          candidates.add(entry.trim());
        }
      }
    }
    candidates.addAll(DEFAULT_NATIVE_LIB_PATHS);

    String existing = System.getProperty("jna.library.path", "");
    List<String> merged = new ArrayList<>();
    if (!existing.isBlank()) {
      merged.add(existing);
    }
    for (String candidate : candidates) {
      if (Files.isDirectory(Paths.get(candidate)) && !merged.contains(candidate)) {
        merged.add(candidate);
      }
    }

    String resolved = String.join(File.pathSeparator, merged);
    if (!resolved.isBlank()) {
      System.setProperty("jna.library.path", resolved);
      log.info("Tesseract native library search path: {}", resolved);
    } else {
      log.warn(
          "No Tesseract native library directory found. OCR will return empty text. "
              + "Install Tesseract (macOS: brew install tesseract) or set app.ocr.native-lib-path.");
    }
  }

  public String extractText(Path filePath, String contentType) {
    if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
      return extractPdfText(filePath.toFile());
    }
    String fileName = filePath.getFileName().toString().toLowerCase();
    if (fileName.endsWith(".pdf")) {
      return extractPdfText(filePath.toFile());
    }
    return extractImageText(filePath.toFile());
  }

  private String extractPdfText(File file) {
    try (PDDocument document = PDDocument.load(file)) {
      PDFTextStripper stripper = new PDFTextStripper();
      String nativeText = normalize(stripper.getText(document));
      if (hasUsefulText(nativeText, MIN_PDF_NATIVE_TEXT_LENGTH)) {
        return nativeText;
      }

      PDFRenderer renderer = new PDFRenderer(document);
      List<String> pages = new ArrayList<>();
      for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
        BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 220);
        String pageText = ocrImage(preprocessImage(pageImage));
        if (!pageText.isBlank()) {
          pages.add(pageText);
        }
      }

      String ocrText = normalize(String.join("\n\n", pages));
      return hasUsefulText(ocrText, MIN_USEFUL_TEXT_LETTERS) ? ocrText : nativeText;
    } catch (IOException ex) {
      log.warn("PDF OCR failed for {}: {}", file.getName(), ex.getMessage());
      return "";
    }
  }

  private String extractImageText(File file) {
    try {
      BufferedImage image = ImageIO.read(file);
      if (image == null) {
        return ocrFile(file);
      }
      return ocrImage(preprocessImage(image));
    } catch (IOException ex) {
      log.warn("Image preprocessing failed for {}: {}", file.getName(), ex.getMessage());
      return ocrFile(file);
    } catch (Throwable ex) {
      // Tess4J can fail through native-loading/linking errors that are not TesseractException.
      log.warn("Image OCR failed unexpectedly for {}", file.getName(), ex);
      return "";
    }
  }

  private String ocrFile(File file) {
    try {
      return normalize(newTesseract().doOCR(file));
    } catch (TesseractException ex) {
      log.warn("Image OCR fallback failed for {}: {}", file.getName(), ex.getMessage());
      return "";
    }
  }

  private String ocrImage(BufferedImage image) {
    try {
      return normalize(newTesseract().doOCR(image));
    } catch (TesseractException ex) {
      log.warn("Buffered image OCR failed: {}", ex.getMessage());
      return "";
    }
  }

  private Tesseract newTesseract() {
    Tesseract tesseract = new Tesseract();
    tesseract.setDatapath(tessdataPath);
    tesseract.setLanguage("eng");
    tesseract.setPageSegMode(6);
    tesseract.setTessVariable("preserve_interword_spaces", "1");
    return tesseract;
  }

  private BufferedImage preprocessImage(BufferedImage source) {
    BufferedImage scaled = upscaleIfNeeded(source);
    BufferedImage gray = new BufferedImage(scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D graphics = gray.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.drawImage(scaled, 0, 0, null);
    graphics.dispose();

    long total = 0;
    int width = gray.getWidth();
    int height = gray.getHeight();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        total += gray.getRaster().getSample(x, y, 0);
      }
    }

    int threshold = (int) Math.max(110, Math.min(200, total / Math.max(1L, (long) width * height)));
    BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int sample = gray.getRaster().getSample(x, y, 0);
        binary.getRaster().setSample(x, y, 0, sample > threshold ? 1 : 0);
      }
    }
    return binary;
  }

  private BufferedImage upscaleIfNeeded(BufferedImage source) {
    int width = source.getWidth();
    int height = source.getHeight();
    if (width >= 1600 || height >= 1600) {
      return source;
    }

    double scale = Math.max(1.6d, 1600d / Math.max(1, Math.max(width, height)));
    int targetWidth = Math.max(width, (int) Math.round(width * scale));
    int targetHeight = Math.max(height, (int) Math.round(height * scale));
    BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = scaled.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    graphics.dispose();
    return scaled;
  }

  private boolean hasUsefulText(String text, int minimumLength) {
    if (text == null || text.isBlank()) {
      return false;
    }
    long letters = text.chars().filter(Character::isLetterOrDigit).count();
    return letters >= minimumLength;
  }

  private String normalize(String input) {
    if (input == null) {
      return "";
    }
    return input
        .replace("\r", "\n")
        .replaceAll("[ \t]{2,}", " ")
        .replaceAll("\n{3,}", "\n\n")
        .trim();
  }
}
