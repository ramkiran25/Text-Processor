package com.textprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.textprocessor.parser.StreamingTextParser;

@Configuration
public class TextProcessorConfig {
  @Bean
  StreamingTextParser streamingTextParser() {
    return new StreamingTextParser();
  }
}
