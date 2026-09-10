package com.textprocessor.controller;

import com.textprocessor.dto.ErrorResponse;
import com.textprocessor.dto.ProcessingRequest;
import com.textprocessor.dto.StatusResponse;
import com.textprocessor.service.TextProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TextProcessingController {

  private final TextProcessingService textProcessingService;

  @PostMapping("/process")
  public ResponseEntity<StreamingResponseBody> process(@RequestParam MultipartFile file,
      @RequestParam String format, @RequestParam(required = false) String outputPath) {

    String normalizedFormat = (format != null) ? format.toLowerCase().trim() : "";
    var request = ProcessingRequest.of(file, normalizedFormat, outputPath);

    MediaType mediaType = resolveMediaType(request.format());
    String fileName = "processed_document." + request.format();

    StreamingResponseBody body = outputStream -> {
      Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      textProcessingService.processToStream(request, writer);
    };

    return ResponseEntity.ok().contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .body(body);
  }

  private static MediaType resolveMediaType(String format) {
    return switch (format) {
      case "xml" -> MediaType.APPLICATION_XML;
      case "csv" -> MediaType.parseMediaType("text/csv");
      default -> throw new IllegalArgumentException("Unsupported format: " + format);
    };
  }
}
