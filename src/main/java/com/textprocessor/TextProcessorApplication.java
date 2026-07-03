package com.textprocessor;

import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.output.strategy.OutputStrategyFactory;
import com.textprocessor.service.ReaderSupplier;
import com.textprocessor.service.TextProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Entry point for the text-processing system — runs in two modes from the same jar:
 *
 * <h2>Mode 1 — REST API (no args)</h2>
 * <pre>
 *   java -jar app.jar
 * </pre>
 * Starts a Tomcat web server. All HTTP endpoints in {@code TextProcessingController}
 * become available on port 8080.
 *
 * <h2>Mode 2 — CLI (args present)</h2>
 * <pre>
 *   java -jar app.jar &lt;xml|csv&gt; [input_file] [output_file]
 *
 *   # File → stdout
 *   java -jar app.jar xml input.txt
 *
 *   # File → file
 *   java -jar app.jar csv input.txt output.csv
 *
 *   # stdin → stdout  (drained to a temp file for two-pass read)
 *   cat input.txt | java -jar app.jar xml
 * </pre>
 * Tomcat is running throughout. The CLI task completes then the app exits normally.
 *
 * <h2>Mode selection</h2>
 * Tomcat always starts. If CLI args are present, {@link #run} processes
 * the file after context refresh and the app exits when done.
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class TextProcessorApplication implements CommandLineRunner {

    private static final int BUFFER_SIZE = 16 * 1024; // 16 KB — matches parser read buffer

    private final TextProcessingService service; // Spring-managed — no manual new()

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SpringApplication.run(TextProcessorApplication.class, args);
    }

    // ── CommandLineRunner ─────────────────────────────────────────────────────

    /**
     * Invoked by Spring after context refresh — only meaningful in CLI mode.
     * In REST API mode this method is still called but exits immediately
     * because {@code args} is empty and no CLI work is needed.
     */
    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            return; // no CLI args — REST API mode, Tomcat handles everything
        }

        CliArgs cliArgs = CliArgs.parse(args);
        OutputStrategy strategy = OutputStrategyFactory.forFormat(cliArgs.format());

        try (InputStage input = InputStage.open(cliArgs.inputFilePath())) {
            log.info("Processing: {} ({} MB)",
                    input.displayName(),
                    String.format("%.2f", input.sizeMb()));

            try (Writer writer = openWriter(cliArgs.outputFilePath())) {
                long startNs = System.nanoTime();
                service.runTwoPasses(input.readerSupplier(), writer, strategy);
                writer.flush();
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

                printBenchmark(input.sizeBytes(), input.sizeMb(), elapsedMs);
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Writer openWriter(String outputFilePath) throws IOException {
        OutputStream out = (outputFilePath != null)
                ? new FileOutputStream(outputFilePath)
                : System.out;
        return new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), BUFFER_SIZE);
    }

    private static void printBenchmark(long bytes, double mb, long ms) {
        log.info("=======================================================");
        log.info("[DATASET]     File size : {} bytes ({} MB)", bytes, String.format("%.2f", mb));
        log.info("[PERFORMANCE] Duration  : {} ms", ms);
        log.info("=======================================================");
    }

    // ── CliArgs — parses and validates the raw args array ────────────────────

    /**
     * Immutable value object that owns CLI argument parsing and validation.
     * Throws {@link IllegalArgumentException} on bad input — callers decide how to handle it.
     */
    record CliArgs(String format, String inputFilePath, String outputFilePath) {

        static CliArgs parse(String[] args) {
            if (args.length < 1) {
                throw new IllegalArgumentException(
                        "Usage: java -jar app.jar <xml|csv> [input_file] [output_file]");
            }

            String format         = args[0].trim().toLowerCase();
            String inputFilePath  = args.length >= 2 ? args[1].trim() : null;
            String outputFilePath = args.length >= 3 ? args[2].trim() : null;

            if (inputFilePath != null) {
                File inputFile = new File(inputFilePath);
                if (!inputFile.exists() || !inputFile.isFile()) {
                    throw new IllegalArgumentException(
                            "Input file '" + inputFilePath + "' does not exist or is not a file.");
                }
            }

            return new CliArgs(format, inputFilePath, outputFilePath);
        }
    }

    // ── InputStage — resolves input source and exposes a ReaderSupplier ──────

    /**
     * Abstracts over the two input modes (physical file vs stdin).
     *
     * <p>When stdin is the source, the stream is drained to a temp file on open
     * and deleted on close. This makes the {@link ReaderSupplier} safely rewindable
     * for the second parse pass without loading the content into heap memory.
     *
     * <p>Always use inside a try-with-resources block.
     */
    static final class InputStage implements Closeable {

        private final File file;
        private final Path tempPath; // non-null only when stdin was drained

        private InputStage(File file, Path tempPath) {
            this.file     = file;
            this.tempPath = tempPath;
        }

        static InputStage open(String inputFilePath) throws IOException {
            if (inputFilePath != null) {
                return new InputStage(new File(inputFilePath), null);
            }

            // Drain stdin → temp file so both passes can rewind to the beginning
            log.info("Reading from stdin — buffering to temp file…");
            Path temp = Files.createTempFile("textproc-stdin-", ".tmp");
            try (InputStream stdin = System.in) {
                Files.copy(stdin, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            return new InputStage(temp.toFile(), temp);
        }

        /** Opens a fresh buffered reader from byte 0 — safe to call once per pass. */
        ReaderSupplier readerSupplier() {
            return () -> new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8),
                    BUFFER_SIZE);
        }

        String displayName() { return (tempPath != null) ? "<stdin>" : file.getName(); }
        long   sizeBytes()   { return file.length(); }
        double sizeMb()      { return sizeBytes() / (1024.0 * 1024.0); }

        @Override
        public void close() throws IOException {
            if (tempPath != null) {
                Files.deleteIfExists(tempPath);
                log.debug("Temp file for stdin deleted: {}", tempPath.getFileName());
            }
        }
    }
}