package com.owndeck.invoicewarranty.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ProjectPathResolver {

  private ProjectPathResolver() {
  }

  public static Path resolveBackendPath(String configuredPath) {
    Path rawPath = Paths.get(configuredPath == null ? "" : configuredPath.trim());
    if (rawPath.isAbsolute()) {
      return rawPath.normalize();
    }

    Path backendRoot = detectBackendRoot();
    return backendRoot.resolve(rawPath).normalize();
  }

  private static Path detectBackendRoot() {
    Path current = Paths.get("").toAbsolutePath().normalize();

    if (looksLikeBackendRoot(current)) {
      return current;
    }

    Path nestedBackend = current.resolve("backend");
    if (looksLikeBackendRoot(nestedBackend)) {
      return nestedBackend;
    }

    return current;
  }

  private static boolean looksLikeBackendRoot(Path path) {
    return Files.exists(path.resolve("pom.xml")) && Files.isDirectory(path.resolve("src/main"));
  }
}
