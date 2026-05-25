package com.textprocessor.model;

import java.util.List;
import lombok.Value;

/**
 * Sentence is a domain value object representing a linguistic sentence within the system
 * architecture. It encapsulates an internally sorted, immutable collection of string tokens.
 * 
 * The class is heavily utilized during Pass 2 of the text processing pipeline, where it acts as a
 * short-lived payload container passed from the streaming event layer (EmitSentenceHandler) to the
 * serialization presentation strategies (XmlOutputStrategy, CsvOutputStrategy). Because it
 * guarantees full deep immutability through a defensive copying strategy, it is entirely safe to
 * serve as a key in hash-based lookup tables (e.g., java.util.Map), fulfilling the Map Key Contract
 * required by complex data mappings.
 */
@Value
public class Sentence {
  List<String> words;

  public Sentence(List<String> words) {
    // Defensive copy — guarantees immutability even if the caller mutates the source list
    this.words = List.copyOf(words);
  }
}
