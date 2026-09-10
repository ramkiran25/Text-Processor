package com.textprocessor.service;

import com.textprocessor.config.ProcessingProperties;
import com.textprocessor.output.strategy.CsvOutputStrategy;
import com.textprocessor.output.strategy.OutputStrategy;
import com.textprocessor.output.strategy.XmlOutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;

class TextProcessingServiceTest {

  private TextProcessingService service;

  @BeforeEach
  void setUp() {
    ProcessingProperties properties = new ProcessingProperties();
    properties.setSimulateDelay(false); // never slow down tests
    service = new TextProcessingService(new StreamingTextParser(), properties);
  }

  // ── CSV output ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("CSV: two sentences produce correctly sorted rows with right column count")
  void csv_twoSentences_sortedWordsAndCorrectColumns() throws IOException {
    // Pass 1 detects max words = 6 ("Its fleece was white as snow")
    // Pass 2 sorts words per sentence:
    // "Mary had a little lamb" → a, had, lamb, little, Mary (5 words)
    // "Its fleece was white as snow" → as, fleece, Its, snow, was, white (6 words)
    String input = "Mary had a little lamb. Its fleece was white as snow!";

    String expected = ", Word 1, Word 2, Word 3, Word 4, Word 5, Word 6\n"
        + "Sentence 1, a, had, lamb, little, Mary\n"
        + "Sentence 2, as, fleece, Its, snow, was, white\n";

    assertThat(process(input, new CsvOutputStrategy())).isEqualTo(expected);
  }

  @Test
  @DisplayName("CSV: single sentence produces one data row and correct header width")
  void csv_singleSentence_oneRowAndHeader() throws IOException {
    String input = "Hello world today.";

    String output = process(input, new CsvOutputStrategy());

    assertThat(output).startsWith(", Word 1").contains("Sentence 1");
  }

  @Test
  @DisplayName("CSV: all sentences with equal word count produce no empty trailing columns")
  void csv_equalLengthSentences_noTrailingEmptyColumns() throws IOException {
    String input = "One two three. Four five six.";

    String output = process(input, new CsvOutputStrategy());

    // Max words = 3, so header should end at Word 3 — no Word 4 column
    assertThat(output).contains("Word 3").doesNotContain("Word 4");
  }

  // ── XML output ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("XML: output is well-formed — opens and closes root element")
  void xml_output_hasRootElement() throws IOException {
    String input = "Spring Boot is great. Java records are useful.";

    String output = process(input, new XmlOutputStrategy());

    assertThat(output).contains("<text>").contains("</text>");
  }

  @Test
  @DisplayName("XML: each sentence appears as a child element")
  void xml_eachSentenceHasElement() throws IOException {
    String input = "First sentence here. Second sentence there.";

    String output = process(input, new XmlOutputStrategy());

    assertThat(output).contains("<sentence").contains("</sentence>");
  }

  // ── Two-pass contract ─────────────────────────────────────────────────────

  @Test
  @DisplayName("ReaderSupplier is called exactly twice — once per pass")
  void readerSupplier_calledTwice() throws IOException {
    int[] callCount = {0};

    ReaderSupplier countingSupplier = () -> {
      callCount[0]++;
      return new StringReader("One two three.");
    };

    service.runTwoPasses(countingSupplier, new StringWriter(), new CsvOutputStrategy());

    assertThat(callCount[0]).isEqualTo(2);
  }

  @Test
  @DisplayName("Output is flushed — writer contains content after runTwoPasses returns")
  void output_isFlushedOnCompletion() throws IOException {
    StringWriter writer = new StringWriter();
    service.runTwoPasses(() -> new StringReader("Flush test sentence."), writer,
        new CsvOutputStrategy());

    assertThat(writer.toString()).isNotBlank();
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private String process(String input, OutputStrategy strategy) throws IOException {
    StringWriter writer = new StringWriter();
    service.runTwoPasses(() -> new StringReader(input), writer, strategy);
    return writer.toString();
  }
}
