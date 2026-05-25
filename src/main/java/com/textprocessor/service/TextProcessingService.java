package com.textprocessor.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import lombok.extern.slf4j.Slf4j;

/**
 * TextProcessingService is the high-level orchestration engine of the application's text-processing
 * pipeline. It implements a deterministic Multi-Pass Streaming Lifecycle designed to process
 * massive text files under extreme memory restrictions, such as a strict 32 MB JVM heap
 * limit.Rather than loading a full document structure into memory, this service coordinates two
 * sequential, low-overhead sweeps over the data stream using a decoupled ReaderSupplier factory. By
 * splitting layout calculation from data transformation, the service maintains a constant O(1)
 * structural memory footprint regardless of the input file size.
 */
@Slf4j
public class TextProcessingService {
  private final StreamingTextParser parser;

  public interface ReaderSupplier {
    Reader get() throws IOException;
  }

  public TextProcessingService(StreamingTextParser parser) {
    this.parser = parser;
  }

  public void process(ReaderSupplier readerSupplier, Writer writer, OutputStrategy strategy)
      throws IOException {
    log.info("Starting multi-pass text streaming processor...");
    int maxWords = 0;

    // Pass 1: Scan stream once to establish header constraints
    try (Reader firstPassReader = readerSupplier.get()) {
      maxWords = parser.findMaxWords(firstPassReader);
    }

    // Pass 2: Re-stream everything directly to destination
    try (Reader secondPassReader = readerSupplier.get()) {
      parser.parseAndStream(secondPassReader, writer, strategy, maxWords);
    }
  }
}
