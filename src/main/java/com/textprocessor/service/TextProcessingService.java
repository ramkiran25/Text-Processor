package com.textprocessor.service;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import com.textprocessor.config.ProcessingProperties;
import com.textprocessor.dto.ProcessingRequest;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.output.strategy.OutputStrategyFactory;
import com.textprocessor.parser.EmitSentenceHandler;
import com.textprocessor.parser.MaxWordsHandler;
import com.textprocessor.parser.StreamingTextParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the two-pass text processing pipeline.

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextProcessingService {

    private static final int BUFFER_SIZE = 16 * 1024;

    private final StreamingTextParser   parser;
    private final ProcessingProperties  properties;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Processes the upload and writes the result to a server-side file at {@code request.outputPath()}.
     */
    public void processToLocalDisk(ProcessingRequest request) throws IOException {
        OutputStrategy strategy = OutputStrategyFactory.forFormat(request.format());
        Path destination        = resolveDestination(request.outputPath());

        try (TempFileStager stager = new TempFileStager(request.file());
             Writer writer         = bufferedWriter(new FileOutputStream(destination.toFile()))) {

            runTwoPasses(stager.readerSupplier(), writer, strategy);
        }

        log.info("Output written to local disk: {}", destination);
    }

    /**
     * Processes the upload and writes the result to {@code responseWriter} (HTTP download stream).
     */
    public void processToStream(ProcessingRequest request, Writer responseWriter) throws IOException {
        OutputStrategy strategy = OutputStrategyFactory.forFormat(request.format());

        try (TempFileStager stager         = new TempFileStager(request.file());
             BufferedWriter bufferedWriter  = new BufferedWriter(responseWriter, BUFFER_SIZE)) {

            runTwoPasses(stager.readerSupplier(), bufferedWriter, strategy);
        }
    }

    // ── Core pipeline ─────────────────────────────────────────────────────────

    /**
     * Executes the two-pass pipeline against any {@link ReaderSupplier}.

     * Pass 1 — scans the entire input to find the maximum sentence word count
     * (required by both output strategies to write header metadata before body rows).
     *
     * Pass 2 — re-reads the input and emits each sentence through the chosen strategy.
     */
    public void runTwoPasses(ReaderSupplier source, Writer output, OutputStrategy strategy)
            throws IOException {

        int maxWords = scanMaxWords(source);
        log.info("Pass 1 complete — max words per sentence: {}", maxWords);

        simulateDelayIfEnabled();

        strategy.startDocument(output, maxWords);

        EmitSentenceHandler emitter = new EmitSentenceHandler(output, strategy);
        try (Reader reader = source.open()) {
            parser.parseStream(reader, emitter);
        }
        emitter.flush();

        strategy.endDocument(output);
        output.flush();

        log.info("Pass 2 complete — output flushed");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int scanMaxWords(ReaderSupplier source) throws IOException {
        MaxWordsHandler handler = new MaxWordsHandler();
        try (Reader reader = source.open()) {
            parser.parseStream(reader, handler);
        }
        return handler.maxWords;
    }


    private void simulateDelayIfEnabled() {
        if (!properties.isSimulateDelay()) return;

        log.debug("Dev delay active: {}ms", properties.getSimulateDelayMs());
        try {
            Thread.sleep(properties.getSimulateDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Path resolveDestination(String rawPath) throws IOException {
        Path destination = Path.of(rawPath);
        if (destination.getParent() != null) {
            Files.createDirectories(destination.getParent());
        }
        return destination;
    }

    private static BufferedWriter bufferedWriter(OutputStream out) {
        return new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), BUFFER_SIZE);
    }
}