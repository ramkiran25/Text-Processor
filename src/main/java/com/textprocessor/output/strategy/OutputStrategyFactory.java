package com.textprocessor.output.strategy;

public final class OutputStrategyFactory {
  private OutputStrategyFactory() {}

  public static OutputStrategy forFormat(String format) {
    return switch (format) {
      case "xml" -> new XmlOutputStrategy();
      case "csv" -> new CsvOutputStrategy();
      default -> throw new IllegalArgumentException("No output strategy for format: " + format);
    };
  }
}
