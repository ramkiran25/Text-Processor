package com.textprocessor.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.textprocessor.model.Sentence;
import com.textprocessor.output.strategy.OutputStrategy;

/**
 * The StreamingTextParser is the core tokenization and orchestration engine of the text processing
 * service. It is designed to process large,text streams efficiently while adhering to strict memory
 * constraints
 */
public class StreamingTextParser {

  private static final int PARSE_BUFFER_SIZE = 16384;
  // A whitelist of known abbreviations to prevent false-positive terminal punctuation
  private final Set<String> knownAbbreviations;

  public StreamingTextParser() {
    // Default constructor using a standard set of common abbreviations
    this(Set.of("Mr", "Mrs", "Ms", "Dr", "St", "etc"));
  }

  public StreamingTextParser(Set<String> knownAbbreviations) {
    this.knownAbbreviations = knownAbbreviations;
  }

  public int findMaxWords(Reader reader) throws IOException {
    int maxWords = 0;
    int currentSentenceWords = 0;
    StringBuilder wordBuilder = new StringBuilder();

    char[] buffer = new char[PARSE_BUFFER_SIZE];
    int charsRead;

    while ((charsRead = reader.read(buffer)) != -1) {
      for (int i = 0; i < charsRead; i++) {
        char c = buffer[i];

        if (c == '.' || c == '!' || c == '?') {
          boolean isTerminal = true;
          if (i + 1 < charsRead) {
            char next = buffer[i + 1];
            if (Character.isLetterOrDigit(next)) {
              isTerminal = false;
            }
          }

          if (c == '.' && isTerminal && isAbbreviation(wordBuilder.toString())) {
            isTerminal = false;
          }

          if (!isTerminal) {
            wordBuilder.append(c);
          } else {
            if (wordBuilder.length() > 0) {
              currentSentenceWords++;
              wordBuilder.setLength(0);
            }
            if (currentSentenceWords > maxWords) {
              maxWords = currentSentenceWords;
            }
            currentSentenceWords = 0;
          }
        } else if (Character.isLetterOrDigit(c) || c == '’' || c == '\'') {
          if (c == '’') {
            wordBuilder.append('\'');
          } else {
            wordBuilder.append(c);
          }
        } else {
          if (wordBuilder.length() > 0) {
            currentSentenceWords++;
            wordBuilder.setLength(0);
          }
        }
      }
    }

    if (wordBuilder.length() > 0) {
      currentSentenceWords++;
    }
    if (currentSentenceWords > maxWords) {
      maxWords = currentSentenceWords;
    }

    return maxWords;
  }

  public void parseAndStream(Reader reader, Writer writer, OutputStrategy strategy, int maxWords)
      throws IOException {
    strategy.startDocument(writer, maxWords);

    List<String> words = new ArrayList<>();
    StringBuilder wordBuilder = new StringBuilder();
    char[] buffer = new char[PARSE_BUFFER_SIZE];
    int charsRead;
    int sentenceIndex = 1;

    while ((charsRead = reader.read(buffer)) != -1) {
      for (int i = 0; i < charsRead; i++) {
        char c = buffer[i];

        if (c == '.' || c == '!' || c == '?') {
          boolean isTerminal = true;
          if (i + 1 < charsRead) {
            char next = buffer[i + 1];
            if (Character.isLetterOrDigit(next)) {
              isTerminal = false;
            }
          }

          if (c == '.' && isTerminal && isAbbreviation(wordBuilder.toString())) {
            isTerminal = false;
          }

          if (!isTerminal) {
            wordBuilder.append(c);
          } else {
            flushWord(wordBuilder, words);
            if (!words.isEmpty()) {
              emitSortedSentence(words, writer, strategy, sentenceIndex++);
            }
          }
        } else if (Character.isLetterOrDigit(c) || c == '’' || c == '\'') {
          if (c == '’') {
            wordBuilder.append('\'');
          } else {
            wordBuilder.append(c);
          }
        } else {
          flushWord(wordBuilder, words);
        }
      }
    }

    flushWord(wordBuilder, words);
    if (!words.isEmpty()) {
      emitSortedSentence(words, writer, strategy, sentenceIndex);
    }

    strategy.endDocument(writer);
    writer.flush();
  }

  private void flushWord(StringBuilder wordBuilder, List<String> words) {
    if (wordBuilder.length() > 0) {
      String token = wordBuilder.toString();
      if (!token.equals("-")) {
        words.add(token);
      }
      wordBuilder.setLength(0);
    }
  }

  private void emitSortedSentence(List<String> words, Writer writer, OutputStrategy strategy,
      int sentenceIndex) throws IOException {
    words.sort((s1, s2) -> {
      int cmp = s1.compareToIgnoreCase(s2);
      return (cmp != 0) ? cmp : s2.compareTo(s1);
    });
    strategy.writeSentence(writer, new Sentence(words), sentenceIndex);
    words.clear();
  }

  private boolean isAbbreviation(String token) {
    return knownAbbreviations.contains(token);
  }
}
