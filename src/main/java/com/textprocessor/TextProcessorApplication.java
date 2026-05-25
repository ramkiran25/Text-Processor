package com.textprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.textprocessor.output.strategy.CsvOutputStrategy;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.output.strategy.XmlOutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import com.textprocessor.service.TextProcessingService;
import lombok.extern.slf4j.Slf4j;

/**
 * TextProcessorApplication serves as the main entry point and Command Line Interface (CLI) driver
 * for the text-processing system. It is responsible for parsing command-line parameters, managing
 * I/O resources, initializing the TextProcessingService engine, and printing execution performance
 * benchmarks to the standard error stream (System.err).
 * 
 * To support a strict 32 MB JVM heap constraint, the architecture relies on a two-pass streaming
 * design. Because a standard Unix pipe (System.in) cannot be natively reset or reread without
 * caching its content, this class intercepts standard input streams and safely drains them onto the
 * local disk as a temporary file. This ensures that the application can execute both
 * stream-analysis passes without blowing out the heap, even when handling multi-gigabyte streams.
 * Command-Line Interface (CLI) Usage The application evaluates positional arguments to configure
 * its input, output, and serialization modes.
 * 
 * Bash # Case 1: Read from a physical file, write directly to standard output java -jar app.jar
 * <xml|csv> <input_file>
 * 
 * # Case 2: Read from a physical file, write directly to a destination file java -jar app.jar
 * <xml|csv> <input_file> <output_file>
 * 
 * # Case 3: Read from standard input (piped stream), write to standard output cat input.txt | java
 * -jar app.jar <xml|csv>
 */
@Slf4j
public class TextProcessorApplication {

  private static final String CSV_TYPE = "csv";
  private static final String XML_TYPE = "xml";
  private static final int BUFFER_SIZE = 65536; // 64KB — matches parser read buffer

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: java -jar app.jar <xml|csv> [input_file] [output_file]");
      System.exit(1);
    }

    String format = args[0].trim().toLowerCase();
    String inputFilePath = (args.length >= 2) ? args[1].trim() : null;
    String outputFilePath = (args.length >= 3) ? args[2].trim() : null;

    // --- Resolve input source -------------------------------------------------
    // If no input file is given, drain stdin to a temp file so we can read it twice.
    File inputFile;
    Path tempFile = null;
    if (inputFilePath != null) {
      inputFile = new File(inputFilePath);
      if (!inputFile.exists() || !inputFile.isFile()) {
        System.err.println("\n=======================================================");
        System.err
            .println("ERROR: Input file '" + inputFilePath + "' does not exist or is invalid.");
        System.err.println("=======================================================\n");
        System.exit(1);
      }
    } else {
      // Stdin mode — buffer to temp file
      tempFile = Files.createTempFile("textproc-", ".tmp");
      inputFile = tempFile.toFile();
      System.err.println("[INFO] Reading from stdin — buffering to temp file...");
      try (InputStream stdin = System.in) {
        Files.copy(stdin, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }
    }

    // --- Strategy & service ---------------------------------------------------
    OutputStrategy strategy = resolveStrategy(format);
    StreamingTextParser parser = new StreamingTextParser();
    TextProcessingService service = new TextProcessingService(parser);

    long fileSizeBytes = inputFile.length();
    double fileSizeMegabytes = fileSizeBytes / (1024.0 * 1024.0);

    System.err.println("[INFO] Processing: " + inputFile.getName() + " ("
        + String.format("%.2f", fileSizeMegabytes) + " MB)");

    // ReaderSupplier reopens the file for each pass (safe for temp file too)
    TextProcessingService.ReaderSupplier fileSupplier = () -> new BufferedReader(
        new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8), BUFFER_SIZE);

    // --- Output destination ---------------------------------------------------
    Writer rawWriter = (outputFilePath != null)
        ? new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8)
        : new OutputStreamWriter(System.out, StandardCharsets.UTF_8);

    try (BufferedWriter outputWriter = new BufferedWriter(rawWriter, BUFFER_SIZE)) {
      long startTime = System.nanoTime();
      service.process(fileSupplier, outputWriter, strategy);
      outputWriter.flush();
      long endTime = System.nanoTime();

      printBenchmark(fileSizeBytes, fileSizeMegabytes, (endTime - startTime) / 1_000_000.0);
    } finally {
      // Clean up temp file if we created one for stdin
      if (tempFile != null) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  private static OutputStrategy resolveStrategy(String format) {
    return switch (format) {
      case XML_TYPE -> new XmlOutputStrategy();
      case CSV_TYPE -> new CsvOutputStrategy();
      default -> throw new IllegalArgumentException(
          "Invalid format: '" + format + "'. Supported formats: xml, csv");
    };
  }

  private static void printBenchmark(long bytes, double mb, double ms) {
    System.err.println("\n=======================================================");
    System.err.println(
        "[DATASET]     File size : " + bytes + " bytes (" + String.format("%.2f", mb) + " MB)");
    System.err.println("[PERFORMANCE] Duration  : " + String.format("%.2f", ms) + " ms");
    System.err.println("=======================================================\n");
  }
}
