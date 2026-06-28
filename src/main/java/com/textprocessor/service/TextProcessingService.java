package com.textprocessor.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.textprocessor.output.strategy.CsvOutputStrategy;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.output.strategy.XmlOutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import com.textprocessor.parser.MaxWordsHandler;
import com.textprocessor.parser.EmitSentenceHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TextProcessingService {

  private static final int BUFFER_SIZE = 16 * 1024;
  private final StreamingTextParser parser;

  public interface ReaderSupplier {
    Reader get() throws IOException;
  }

  public TextProcessingService(StreamingTextParser parser) {
    this.parser = parser;
  }

  public void processMultiPartFile(MultipartFile multiPartFile, Writer responseWriter,
      String format, String outputPath) throws IOException {
    Path tempFile = null;

    try {
      OutputStrategy strategy = switch (format) {
        case "xml" -> new XmlOutputStrategy();
        case "csv" -> new CsvOutputStrategy();
        default -> throw new IllegalArgumentException(
            "Unsupported transformation format target: " + format);
      };

      tempFile = Files.createTempFile("upload-textproc-", ".tmp");
      File inputFile = tempFile.toFile();

      log.info("Staging incoming multipart track onto local system workspace disk: {}",
          inputFile.getName());
      try (var inputStream = multiPartFile.getInputStream()) {
        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }

      ReaderSupplier fileSupplier = () -> new BufferedReader(
          new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8),
          BUFFER_SIZE);

      boolean isLocalWrite = outputPath != null && !outputPath.trim().isEmpty();

      if (isLocalWrite) {
        File destinationFile = new File(outputPath.trim());
        File parentDir = destinationFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
          parentDir.mkdirs();
        }

        // Wrapped safely inside its own try-with-resources block
        try (FileOutputStream fos = new FileOutputStream(destinationFile);
            Writer targetWriter = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter bufferedOutputWriter = new BufferedWriter(targetWriter, BUFFER_SIZE)) {
          this.process(fileSupplier, bufferedOutputWriter, strategy);
          bufferedOutputWriter.flush();
        }
      } else {
        // Standard browser download path
        try (
            BufferedWriter bufferedOutputWriter = new BufferedWriter(responseWriter, BUFFER_SIZE)) {
          this.process(fileSupplier, bufferedOutputWriter, strategy);
          bufferedOutputWriter.flush();
        }
      }

      log.info("Transformation finished successfully. Destination target local system write? {}",
          isLocalWrite);
    } finally {
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
          log.info("Staging filesystem workspace isolated and cleared safely.");
        } catch (IOException e) {
          log.warn("Non-fatal error cleaning temporary files from workspace storage: {}", tempFile,
              e);
        }
      }
    }
  }

  public void process(ReaderSupplier readerSupplier, Writer writer, OutputStrategy strategy)
      throws IOException {
    log.info("Starting multi-pass text streaming processor...");

    MaxWordsHandler maxWordsHandler = new MaxWordsHandler();
    try (Reader firstPassReader = readerSupplier.get()) {
      parser.parseStream(firstPassReader, maxWordsHandler);
    }
    int maxWords = maxWordsHandler.maxWords;
    log.info("Pass 1 completed. Calculated Max Words dimension: {}", maxWords);

    try {
      log.info("DEV MODE: Injecting a 3-second delay to verify frontend spinner animation...");
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    strategy.startDocument(writer, maxWords);

    EmitSentenceHandler emitHandler = new EmitSentenceHandler(writer, strategy);
    try (Reader secondPassReader = readerSupplier.get()) {
      parser.parseStream(secondPassReader, emitHandler);
    }
    emitHandler.flush();
    strategy.endDocument(writer);
    writer.flush();

    log.info("Pass 2 completed. Output pipeline successfully flushed.");
  }
}
