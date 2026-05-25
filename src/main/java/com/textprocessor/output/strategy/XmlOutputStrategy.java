package com.textprocessor.output.strategy;

import java.io.IOException;
import java.io.Writer;
import com.textprocessor.model.Sentence;

/**
 * XML presentation strategy.
 */
public class XmlOutputStrategy implements OutputStrategy {

  @Override
  public void startDocument(Writer writer, int maxWords) throws IOException {
    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
    writer.write("<text>\n");
  }

  @Override
  public void writeSentence(Writer writer, Sentence sentence, int sentenceIndex)
      throws IOException {
    writer.write("<sentence>");
    for (String word : sentence.getWords()) {
      writer.write("<word>");
      writer.write(escapeXml(word));
      writer.write("</word>");
    }
    writer.write("</sentence>\n");
  }

  @Override
  public void endDocument(Writer writer) throws IOException {
    writer.write("</text>");
  }

  /**
   * Escapes XML special characters. Takes a zero-allocation fast path for the common case (words
   * with no special characters), only creating a StringBuilder when escaping is needed.
   */
  private static String escapeXml(String input) {
    if (input == null)
      return "";

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
    if (specialAt == -1)
      return input; // no special chars — zero allocation

    // Slow path — only reached when escaping is actually needed
    StringBuilder sb = new StringBuilder(len + 8);
    sb.append(input, 0, specialAt); // copy clean prefix
    for (int i = specialAt; i < len; i++) {
      char c = input.charAt(i);
      switch (c) {
        case '&':
          sb.append("&amp;");
          break;
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '"':
          sb.append("&quot;");
          break;
        case '\'':
          sb.append("&apos;");
          break;
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }
}
