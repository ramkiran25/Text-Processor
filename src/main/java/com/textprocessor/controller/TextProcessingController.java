package com.textprocessor.controller;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.textprocessor.service.TextProcessingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TextProcessingController {

  private final TextProcessingService textProcessingService;

  @PostMapping(value = "/process")
  public ResponseEntity<?> processUploadedFile(
      @RequestParam("file") MultipartFile multipartFile,
      @RequestParam("format") String format,
      @RequestParam(value = "outputPath", required = false) String outputPath,
      HttpServletResponse response) throws IOException {

    String requestedFormat = format.trim().toLowerCase();
    boolean isLocalWrite = outputPath != null && !outputPath.trim().isEmpty();

    try {
      // SCENARIO A: User specified a local disk path
      if (isLocalWrite) {
        // Pass null for response writer since it saves directly to disk
        textProcessingService.processMultiPartFile(multipartFile, null, requestedFormat, outputPath);
        log.info("Local path file serialization finished cleanly to: {}", outputPath);
        
        // Return explicit valid JSON to clear Angular's parser
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"status\":\"SUCCESS\",\"message\":\"Saved locally to disk\"}");
      }

      // SCENARIO B: Standard browser attachment download stream
      if ("xml".equals(requestedFormat)) {
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
      } else if ("csv".equals(requestedFormat)) {
        response.setContentType("text/csv");
      } else {
        throw new IllegalArgumentException("Unsupported format type: " + requestedFormat);
      }

      response.setCharacterEncoding("UTF-8");
      String outputFileName = "processed_document." + requestedFormat;
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFileName + "\"");

      textProcessingService.processMultiPartFile(multipartFile, response.getWriter(), requestedFormat, outputPath);
      log.info("Streaming conversion successfully flushed to browser download pipeline.");
      
      return null; // Servlet response stream takes over the execution lifecycle cleanly
    } catch (IllegalArgumentException e) {
      log.error("Request validation failed: {}", e.getMessage());
      return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
    } catch (Exception e) {
      log.error("Unhandled exception during streaming execution: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("{\"error\":\"Internal server processing pipeline failure\"}");
    }
  }
}