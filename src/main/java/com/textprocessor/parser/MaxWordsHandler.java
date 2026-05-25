package com.textprocessor.parser;

/*
 * MaxWordsHandler is a stateful, high-performance event consumer that implements the TokenHandler
 * interface. It is specifically designed to operate during the first pass of the text processing
 * lifecycle inside the TextProcessingService.
 * 
 * Its sole responsibility is to compute the maximum number of words contained within any single
 * sentence across an entire stream of text. To achieve this under severe memory restrictions (such
 * as a 32 MB strict heap constraint), the class maintains primitive numerical counters rather than
 * allocating or buffering string tokens. This allows the processor to determine the tabular
 * dimension constraints of massive data sets with a constant O(1) space complexity.
 */
public class MaxWordsHandler implements TokenHandler {
  int maxWords = 0;
  private int current = 0;

  @Override
  public void onWord(char[] buf, int len) {
    // Avoid tracking empty strings or standalone hyphen tokens as words
    if (len == 0 || (len == 1 && buf[0] == '-'))
      return;
    current++;
  }

  @Override
  public void onSentenceEnd() {
    if (current > maxWords)
      maxWords = current;
    current = 0;
  }

}
