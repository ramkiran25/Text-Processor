package com.textprocessor.output.strategy;

import java.io.IOException;
import java.io.Writer;
import com.textprocessor.model.Sentence;

/**
 * XML presentation strategy.
 */
public class XmlOutputStrategy implements OutputStrategy {

  private static final String XML_HEADER =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
  private static final String ROOT_START = "<text>\n";
  private static final String ROOT_END = "</text>";
  private static final String SENTENCE_START = "<sentence>";
  private static final String SENTENCE_END = "</sentence>\n";
  private static final String WORD_START = "<word>";
  private static final String WORD_END = "</word>";

  @Override
  public void startDocument(Writer writer, int maxWords) throws IOException {
    writer.write(XML_HEADER);
    writer.write(ROOT_START);
  }

  @Override
  public void writeSentence(Writer writer, Sentence sentence, int sentenceIndex)
      throws IOException {
    writer.write(SENTENCE_START);
    for (String word : sentence.getWords()) {
      writer.write(WORD_START);
      writer.write(escapeXml(word));
      writer.write(WORD_END);
    }
    writer.write(SENTENCE_END);
  }

  @Override
  public void endDocument(Writer writer) throws IOException {
    writer.write(ROOT_END);
  }

  /**
   * Escapes XML special characters. Takes a zero-allocation fast path for the common case (words
   * with no special characters), only creating a StringBuilder when escaping is needed.
   */
  private static String escapeXml(String input) {
    if (input == null) {
      return "";
    }

    // Fast path — scan first; build only if necessary
    int len = input.length();
    int specialAt = -1;
    for (int i = 0; i < len; i++) {
      char c = input.charAt(i);
      if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
        specialAt = i;
        break;
      }
    }

    if (specialAt == -1) {
      return input; // no special chars — zero allocation
    }

    // Slow path — only reached when escaping is actually needed
    StringBuilder sb = new StringBuilder(len + 8);
    sb.append(input, 0, specialAt); 

    for (int i = specialAt; i < len; i++) {
      char c = input.charAt(i);
      sb.append(switch (c) {
        case '&' -> "&amp;";
        case '<' -> "&lt;";
        case '>' -> "&gt;";
        case '"' -> "&quot;";
        case '\'' -> "&apos;";
        default -> c;
      });
    }
    return sb.toString();
  }
}
