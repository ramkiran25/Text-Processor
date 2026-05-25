package com.textprocessor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.textprocessor.model.Sentence;
import com.textprocessor.output.strategy.CsvOutputStrategy;
import com.textprocessor.output.strategy.XmlOutputStrategy;
import com.textprocessor.parser.StreamingTextParser;
import com.textprocessor.service.TextProcessingService;

public class TextProcessorTest {

  // ---------------------------------------------------------------------------
  // StreamingTextParser — findMaxWords
  // ---------------------------------------------------------------------------

  @DisplayName("FindMaxWords — standard two-sentence input")
  @Test
  public void testFindMaxWordsCountsCorrectly() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    String input = "Mary had a little lamb. Peter called for the wolf, and Aesop came.";
    try (StringReader reader = new StringReader(input)) {
      assertEquals(8, parser.findMaxWords(reader));
    }
  }

  @DisplayName("FindMaxWords — trailing text without terminal punctuation")
  @Test
  public void testFindMaxWordsTrailingText() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    // "one two three" has no terminal mark — must still be counted
    String input = "Hi there. one two three";
    try (StringReader reader = new StringReader(input)) {
      assertEquals(3, parser.findMaxWords(reader));
    }
  }

  // ---------------------------------------------------------------------------
  // StreamingTextParser — abbreviation handling
  // ---------------------------------------------------------------------------

  @DisplayName("Abbreviation — Mr. does not split sentence")
  @Test
  public void testMrAbbreviationDoesNotSplitSentence() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    XmlOutputStrategy xml = new XmlOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    String input = "Mr. Smith went home.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, xml);

    String result = out.toString().replaceAll("\\s+", "");
    assertTrue(result.contains("<word>Mr.</word>"), "Mr. should be preserved as one token");
    assertTrue(result.contains("<word>Smith</word>"));
    assertTrue(result.contains("<word>home</word>"));
  }

  @DisplayName("Abbreviation — Dr. does not split sentence")
  @Test
  public void testDrAbbreviationDoesNotSplitSentence() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    XmlOutputStrategy xml = new XmlOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    String input = "Dr. Jones prescribed rest.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, xml);

    String result = out.toString().replaceAll("\\s+", "");
    assertTrue(result.contains("<word>Dr.</word>"));
    assertTrue(result.contains("<word>Jones</word>"));
    assertTrue(result.contains("<word>rest</word>"));
  }

  // ---------------------------------------------------------------------------
  // XmlOutputStrategy
  // ---------------------------------------------------------------------------

  @DisplayName("XML Strategy — output format matches strict spec (no indentation)")
  @Test
  public void testXmlOutputMatchesSpecFormat() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    XmlOutputStrategy xml = new XmlOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    String input = "Mary had a little lamb.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, xml);

    String output = out.toString();

    // Assert that the output does NOT contain indentation
    assertFalse(output.contains("    "), "Output should not contain 4-space indentation");
    assertFalse(output.contains("        "), "Output should not contain 8-space indentation");

    // Assert structural integrity
    assertTrue(output.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"),
        "XML Header missing");
    assertTrue(output.contains("<sentence>"), "Sentence tag missing");
    assertTrue(output.contains("<word>a</word>"), "Word tag missing");
    assertTrue(output.endsWith("</text>"), "Closing tag missing");
  }

  @DisplayName("XML — words sorted case-insensitively within sentence")
  @Test
  public void testXmlWordSortOrder() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    XmlOutputStrategy xml = new XmlOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    // Expected sort (case-insensitive): a, had, lamb, little, Mary
    String input = "Mary had a little lamb.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, xml);

    String result = out.toString();
    int posA = result.indexOf("<word>a</word>");
    int posHad = result.indexOf("<word>had</word>");
    int posLamb = result.indexOf("<word>lamb</word>");
    int posLittle = result.indexOf("<word>little</word>");
    int posMary = result.indexOf("<word>Mary</word>");

    assertTrue(posA < posHad, "a before had");
    assertTrue(posHad < posLamb, "had before lamb");
    assertTrue(posLamb < posLittle, "lamb before little");
    assertTrue(posLittle < posMary, "little before Mary");
  }

  @DisplayName("XML — character escaping sanitisation")
  @Test
  public void testXmlCharacterEscapingSanitization() throws Exception {
    XmlOutputStrategy xml = new XmlOutputStrategy();
    StringWriter out = new StringWriter();
    xml.writeSentence(out, new Sentence(List.of("Apple", "&", "Banana", "<fresh>")), 1);

    String result = out.toString();
    assertTrue(result.contains("&amp;"), "ampersand must be escaped");
    assertTrue(result.contains("&lt;"), "less-than must be escaped");
    assertTrue(result.contains("&gt;"), "greater-than must be escaped");
    assertFalse(result.contains("<word>&</word>"), "raw & must not appear");
    assertFalse(result.contains("<word><</word>"), "raw < must not appear");
  }

  @DisplayName("XML — edge case: whitespace-only / no terminal punctuation")
  @Test
  public void testXmlEdgeCaseNoTerminalPunctuation() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    XmlOutputStrategy xml = new XmlOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    String input = "   Unfinished   sentence   data  testing  ";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, xml);

    String result = out.toString().replaceAll("\\s+", "");
    assertTrue(result.contains("<word>data</word>"));
    assertTrue(result.contains("<word>sentence</word>"));
    assertTrue(result.contains("<word>testing</word>"));
    assertTrue(result.contains("<word>Unfinished</word>"));
  }

  // ---------------------------------------------------------------------------
  // CsvOutputStrategy
  // ---------------------------------------------------------------------------

  @DisplayName("CSV — header columns match maxWords")
  @Test
  public void testCsvHeaderMatchesMaxWords() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    CsvOutputStrategy csv = new CsvOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    // Sentence 2 has 8 words — header must go up to Word 8
    String input = "Mary had a little lamb. Peter called for the wolf, and Aesop came.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, csv);

    String header = out.toString().split("\n")[0];
    assertTrue(header.contains("Word 8"), "header must include Word 8 (max sentence length)");
    assertFalse(header.contains("Word 9"), "header must not exceed maxWords");
  }

  @DisplayName("CSV — content tokens present")
  @Test
  public void testCsvContentTokensPresent() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    CsvOutputStrategy csv = new CsvOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    String input = "Mary had a little lamb. Peter called for the wolf, and Aesop came.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, csv);

    String result = out.toString();
    assertTrue(result.contains("Sentence 1"));
    assertTrue(result.contains("Sentence 2"));
    assertTrue(result.contains("Mary"));
    assertTrue(result.contains("lamb"));
    assertTrue(result.contains("Aesop"));
    assertTrue(result.contains("wolf"));
  }

  // ---------------------------------------------------------------------------
  // Sentence — Map key contract
  // ---------------------------------------------------------------------------

  @DisplayName("Sentence — equals and hashCode consistent (Map key contract)")
  @Test
  public void testSentenceEqualsAndHashCode() {
    Sentence s1 = new Sentence(List.of("apple", "banana"));
    Sentence s2 = new Sentence(List.of("apple", "banana"));
    Sentence s3 = new Sentence(List.of("banana", "apple"));

    assertEquals(s1, s2, "same word order → equal");
    assertEquals(s1.hashCode(), s2.hashCode(), "equal objects must have equal hashCodes");
    assertFalse(s1.equals(s3), "different word order → not equal");
  }

  @DisplayName("Sentence — words list is immutable after construction")
  @Test
  public void testSentenceImmutability() {
    java.util.List<String> mutable = new java.util.ArrayList<>(List.of("a", "b"));
    Sentence sentence = new Sentence(mutable);

    mutable.add("c"); // mutate the source list

    assertEquals(2, sentence.getWords().size(),
        "Sentence word list must not reflect changes to the original list");
  }

  @DisplayName("CSV Strategy — short sentences truncate cleanly without trailing padding commas")
  @Test
  public void testCsvShortRowsPaddedCorrectly() throws Exception {
    StreamingTextParser parser = new StreamingTextParser();
    CsvOutputStrategy csv = new CsvOutputStrategy();
    TextProcessingService service = new TextProcessingService(parser);

    String input = "Mary had a little lamb. Peter called for the wolf, and Aesop came.";
    StringWriter out = new StringWriter();
    service.process(() -> new StringReader(input), out, csv);

    String[] lines = out.toString().split("\n");
    String headerLine = lines[0];
    String sentence1Row = lines[1];

    // Count commas:
    // Header: ", Word 1, Word 2, Word 3, Word 4, Word 5, Word 6, Word 7, Word 8"
    // This string contains 8 commas.
    long headerCommas = headerLine.chars().filter(c -> c == ',').count();

    // Sentence 1: "Sentence 1, a, had, lamb, little, Mary"
    // This row contains 5 commas.
    long row1Commas = sentence1Row.chars().filter(c -> c == ',').count();

    assertEquals(8, headerCommas, "Header should have 8 commas for 8 words");
    assertEquals(5, row1Commas, "Sentence 1 row should have 5 commas (no padding)");

    assertFalse(sentence1Row.endsWith(","), "Sentence 1 row must not end with trailing padding");
  }
}
