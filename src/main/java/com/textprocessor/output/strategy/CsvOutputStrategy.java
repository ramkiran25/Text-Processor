package com.textprocessor.output.strategy;

import java.io.IOException;
import java.io.Writer;
import com.textprocessor.model.Sentence;

/**
 * CSV presentation strategy matching the exact expected reference output structure.
 */
public class CsvOutputStrategy implements OutputStrategy {

  @Override
  public void startDocument(Writer writer, int maxWords) throws IOException {
    writer.write(",");
    for (int i = 1; i <= maxWords; i++) {
      writer.write(" Word " + i + (i < maxWords ? "," : ""));
    }
    writer.write("\n");
  }

  @Override
  public void writeSentence(Writer writer, Sentence sentence, int sentenceIndex)
      throws IOException {
    writer.write("Sentence " + sentenceIndex);
    for (String word : sentence.getWords()) {
      writer.write(", " + word);
    }
    writer.write("\n");
  }
}
