package com.textprocessor.service;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TempFileStager implements Closeable {
  private static final int BUFFER_SIZE = 16 * 1024;

  private final Path tempPath;

  public TempFileStager(MultipartFile source) throws IOException {
    tempPath = Files.createTempFile("textproc-", ".tmp");
    log.debug("Staging multipart upload to temp file: {}", tempPath.getFileName());

    try (InputStream in = source.getInputStream()) {
      Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Returns a {@link ReaderSupplier} backed by the staged temp file. Each call to
   * {@link ReaderSupplier#open()} opens a new buffered reader from the beginning of the file — safe
   * to call multiple times (one per pass).
   */
  public ReaderSupplier readerSupplier() {
    return () -> new BufferedReader(
        new InputStreamReader(new FileInputStream(tempPath.toFile()), StandardCharsets.UTF_8),
        BUFFER_SIZE);
  }

  @Override
  public void close() throws IOException {
    try {
      Files.deleteIfExists(tempPath);
      log.debug("Temp file deleted: {}", tempPath.getFileName());
    } catch (IOException e) {
      log.warn("Could not delete temp file {}: {}", tempPath, e.getMessage());
    }

  }

}
