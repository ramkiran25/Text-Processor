package com.textprocessor.output.strategy;

import java.io.IOException;
import java.io.Writer;
import com.textprocessor.model.Sentence;

/**
 * Defines the structural presentation lifecycle contract for data transformation. Part of the
 * Strategy Design Pattern to decouple core stream parsing from specific output layouts like XML or
 * CSV.
 */
public interface OutputStrategy {
  void startDocument(Writer writer, int maxWords) throws IOException;

  void writeSentence(Writer writer, Sentence sentence, int sentenceIndex) throws IOException;

  default void endDocument(Writer writer) throws IOException {

  }
}
