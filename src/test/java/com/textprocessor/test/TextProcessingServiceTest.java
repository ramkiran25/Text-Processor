package com.textprocessor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.textprocessor.output.strategy.CsvOutputStrategy;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import com.textprocessor.service.TextProcessingService;

class TextProcessingServiceTest {

  private TextProcessingService service;

  @BeforeEach
  void setUp() {
    // Instantiate the real streaming parser with default abbreviations
    StreamingTextParser parser = new StreamingTextParser();
    service = new TextProcessingService(parser);
  }

  @Test
  @DisplayName("Should successfully execute two-pass processing and generate sorted CSV output")
  void shouldProcessMultiPassTextStreamingToCsv() throws IOException {
    // Arrange: Define a sample text payload with varying sentence lengths and unsorted words
    String sampleInput = "Mary had a little lamb. Its fleece was white as snow!";
    
    // Provide a ReaderSupplier that resets the stream for each pass
    TextProcessingService.ReaderSupplier readerSupplier = () -> new StringReader(sampleInput);
    
    StringWriter outputWriter = new StringWriter();
    OutputStrategy csvStrategy = new CsvOutputStrategy();

    // Act: Run the multi-pass text processing service
    service.process(readerSupplier, outputWriter, csvStrategy);

    // Assert: Verify against expected exact reference CSV layout
    // Pass 1 should detect max words = 5 ("Its fleece was white as snow")
    // Pass 2 should sort words: 
    //   - "Mary had a little lamb" -> a, had, lamb, little, Mary
    //   - "Its fleece was white as snow" -> as, fleece, Its, snow, was, white
    String expectedOutput = 
        ", Word 1, Word 2, Word 3, Word 4, Word 5, Word 6\n" +
        "Sentence 1, a, had, lamb, little, Mary\n" +
        "Sentence 2, as, fleece, Its, snow, was, white\n";

    assertEquals(expectedOutput, outputWriter.toString());
  }
}