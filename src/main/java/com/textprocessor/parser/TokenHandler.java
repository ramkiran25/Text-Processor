package com.textprocessor.parser;

import java.io.IOException;

/*
 * TokenHandler is a high-performance, low-level event listener contract designed for
 * stream-oriented text processing. It defines the callback lifecycle hooks that a streaming state
 * machine (such as StreamingTextParser) invokes when discovering basic semantic structures—namely
 * words and sentence boundaries.
 * 
 * By employing a zero-allocation design paradigm where raw mutable segments (char[]) are passed
 * directly from the read buffer to subscribers, this interface allows for a multi-pass pipeline.
 * Implementations can either completely ignore data allocations to compute statistics (Pass 1) or
 * selectively bundle tokens into short-lived models to stream transformations immediately to a
 * writer (Pass 2). This architecture avoids holding large documents in memory, making it ideal for
 * resource-constrained systems operating under a strict 32 MB Heap Limit.
 */
public interface TokenHandler {
  void onWord(char[] buf, int len);

  void onSentenceEnd() throws IOException;
}
