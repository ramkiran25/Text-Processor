package com.textprocessor.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * High-performance, memory-safe streaming tokenizer. Operates with O(1) space complexity by
 * streaming characters directly into a reusable buffer and broadcasting events to a TokenHandler.
 */
public class StreamingTextParser {

  private static final int PARSE_BUFFER_SIZE = 16 * 1024;// 16KB
  private static final int MAX_WORD_LENGTH = 2048;

  private final Set<String> knownAbbreviations;

  public StreamingTextParser() {
    this(Set.of("Mr", "Mrs", "Ms", "Dr", "St", "etc"));
  }

  public StreamingTextParser(Set<String> knownAbbreviations) {
    this.knownAbbreviations = knownAbbreviations;
  }

  public void parseStream(Reader reader, TokenHandler handler) throws IOException {
    char[] buffer = new char[PARSE_BUFFER_SIZE];
    char[] wordBuffer = new char[MAX_WORD_LENGTH];

    int wordLen = 0;
    int wordsInCurrentSentence = 0;
    int charsRead;

    while ((charsRead = reader.read(buffer)) != -1) {
      for (int i = 0; i < charsRead; i++) {
        char c = buffer[i];

        if (Character.isLetterOrDigit(c)) {
          wordLen = appendCharacter(wordBuffer, wordLen, c);
        } else {
          if (wordLen > 0) {
            String wordStr = new String(wordBuffer, 0, wordLen);
            handler.onWord(wordBuffer, wordLen);
            wordsInCurrentSentence++;
            wordLen = 0;

            if (c == '.' && knownAbbreviations.contains(wordStr)) {
              continue;
            }
          }

          switch (c) {
            case '.', '?', '!' -> {
              if (wordsInCurrentSentence > 0) {
                handler.onSentenceEnd();
                wordsInCurrentSentence = 0;
              }
            }
            default -> {
              /* Skip whitespace or normal non-word delimiter characters */ }
          }
        }
      }
    }
    // Flush any leftover word stuck in the trailing buffer at EOF
    if (wordLen > 0) {
      handler.onWord(wordBuffer, wordLen);
      wordsInCurrentSentence++;
    }

    // Guarantee a closing boundary signal for the document if data was parsed
    if (wordsInCurrentSentence > 0) {
      handler.onSentenceEnd();
    }
  }

  private int appendCharacter(char[] wordBuffer, int wordLen, char c) {
    if (wordLen < wordBuffer.length) {
      wordBuffer[wordLen++] = c;
    }
    return wordLen;
  }
}
