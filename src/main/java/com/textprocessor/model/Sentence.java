package com.textprocessor.model;

import java.util.List;
import lombok.Value;

/**
 * Sentence is a domain value object representing a linguistic sentence within the system
 * architecture. It encapsulates an internally sorted, immutable collection of string tokens.
 */
@Value
public class Sentence {
  List<String> words;

  public Sentence(List<String> words) {
    // Defensive copy — guarantees immutability even if the caller mutates the source list
    this.words = List.copyOf(words);
  }
}
