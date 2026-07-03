package com.textprocessor.dto;

import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public record ProcessingRequest(MultipartFile file, String format, String outputPath) {
  private static final Set<String> SUPPORTED_FORMATS = Set.of("xml", "csv");

  /** Factory — validates and normalises raw controller parameters. */
  public static ProcessingRequest of(MultipartFile file, String rawFormat, String rawOutputPath) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file must not be empty");
    }

    String format = normalise(rawFormat);
    if (!SUPPORTED_FORMATS.contains(format)) {
      throw new IllegalArgumentException(
          "Unsupported format '" + rawFormat + "'. Supported: " + SUPPORTED_FORMATS);
    }

    String outputPath =
        (rawOutputPath != null && !rawOutputPath.isBlank()) ? rawOutputPath.trim() : null;

    return new ProcessingRequest(file, format, outputPath);
  }

  /** Returns {@code true} when the caller wants output written to server-side disk. */
  public boolean isLocalWrite() {
    return outputPath != null;
  }

  private static String normalise(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("Format parameter must not be blank");
    }
    return raw.trim().toLowerCase();
  }
}
