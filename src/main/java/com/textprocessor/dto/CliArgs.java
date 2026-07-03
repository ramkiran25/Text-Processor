package com.textprocessor.dto;

import java.io.File;

public record CliArgs(String format, String inputFilePath, String outputFilePath) {
  public static CliArgs parse(String[] args) {
    if (args.length < 1) {
      throw new IllegalArgumentException(
          "Usage: java -jar app.jar <xml|csv> [input_file] [output_file]");
    }
    String format = args[0].trim().toLowerCase();
    String inputFilePath = args.length >= 2 ? args[1].trim() : null;
    String outputFilePath = args.length >= 3 ? args[2].trim() : null;

    if (inputFilePath != null) {
      File inputFile = new File(inputFilePath);
      if (!inputFile.exists() || !inputFile.isFile()) {
        throw new IllegalArgumentException(
            "Input file '" + inputFilePath + "' does not exist or is not a file.");
      }
    }

    return new CliArgs(format, inputFilePath, outputFilePath);
  }
}
