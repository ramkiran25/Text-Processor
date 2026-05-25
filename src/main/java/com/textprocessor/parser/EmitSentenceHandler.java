package com.textprocessor.parser;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import com.textprocessor.model.Sentence;
import com.textprocessor.output.strategy.OutputStrategy;

/*
 * EmitSentenceHandler is a stateful event-driven lifecycle handler that implements the TokenHandler
 * interface. It is responsible for intercepting text parsing streaming events (onWord and
 * onSentenceEnd) during the second pass of the TextProcessingService pipeline.
 * 
 * Instead of buffering an entire file into memory, EmitSentenceHandler holds only a single sentence
 * context at any given time. Once a sentence boundary is encountered, it applies alphabetical
 * sorting transformations, encapsulates the data into an immutable domain model, flushes the result
 * to the output stream via a specified OutputStrategy, and completely purges its local cache. This
 * localized collection window guarantees that the heap footprint remains bound to the size of the
 * longest single sentence, making it perfectly safe for environments operating under strict memory
 * constraints (e.g., a 32 MB JVM Heap Limit).
 * 
 */
public class EmitSentenceHandler implements TokenHandler {
  private final Writer writer;
  private final OutputStrategy strategy;
  private final List<String> words = new ArrayList<>();
  private int sentenceIndex = 1;

  EmitSentenceHandler(Writer writer, OutputStrategy strategy) {
    this.writer = writer;
    this.strategy = strategy;
  }

  @Override
  public void onWord(char[] buf, int len) {
    if (len == 0 || (len == 1 && buf[0] == '-'))
      return;
    words.add(new String(buf, 0, len));
  }

  @Override
  public void onSentenceEnd() throws IOException {
    if (!words.isEmpty()) {
      emitSortedSentence();
    }
  }

  void flush() throws IOException {
    onSentenceEnd();
  }

  private void emitSortedSentence() throws IOException {
    words.sort((s1, s2) -> {
      int cmp = s1.compareToIgnoreCase(s2);
      return (cmp != 0) ? cmp : s2.compareTo(s1);
    });
    Sentence sentence = new Sentence(new ArrayList<>(words));
    strategy.writeSentence(writer, sentence, sentenceIndex++);
    words.clear();
  }

}
