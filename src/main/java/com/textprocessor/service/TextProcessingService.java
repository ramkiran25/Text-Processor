package com.textprocessor.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.springframework.stereotype.Service;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import com.textprocessor.parser.MaxWordsHandler;
import com.textprocessor.parser.EmitSentenceHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * TextProcessingService coordinates two sequential, low-overhead sweeps 
 * over the data stream using a decoupled ReaderSupplier factory.
 */
@Slf4j
@Service
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
    
    // Pass 1: Scan stream via event hooks to establish structural tabular metrics
    MaxWordsHandler maxWordsHandler = new MaxWordsHandler();
    try (Reader firstPassReader = readerSupplier.get()) {
      parser.parseStream(firstPassReader, maxWordsHandler);
    }

    int maxWords = maxWordsHandler.maxWords;
    log.info("Pass 1 completed. Calculated Max Words dimension: {}", maxWords);

    // Initialize the strategy layout structure (e.g., CSV Headers, XML roots)
    strategy.startDocument(writer, maxWords);

    // Pass 2: Re-stream everything through the transformer to final output destination
    EmitSentenceHandler emitHandler = new EmitSentenceHandler(writer, strategy);
    try (Reader secondPassReader = readerSupplier.get()) {
      parser.parseStream(secondPassReader, emitHandler);
    }
    
    // Ensure any trailing data buffers or structural closings are finalized
    emitHandler.flush();
    strategy.endDocument(writer);
    writer.flush();
    
    log.info("\n\nPass 2 completed. Output pipeline successfully flushed.\n");
  }
}