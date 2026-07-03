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

/**
 * Thin REST controller — owns only HTTP concerns: request binding, response shape, status codes,
 * and error mapping.
 *
 * All processing decisions live in {@link TextProcessingService}.
 */
@Slf4j
@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:4200}")
public class TextProcessingController {

  private final TextProcessingService textProcessingService;

  /**
   * Accepts a plain-text file and transforms it to the requested format (xml | csv).
   *
   * <p>
   * Two output modes driven by {@code outputPath}:
   * <ul>
   * <li>If {@code outputPath} is present → write to server-side disk, return 200 + status
   * body.</li>
   * <li>If absent → stream the transformed content as a file download.</li>
   * </ul>
   */
  @PostMapping("/process")
  public ResponseEntity<?> process(@RequestParam("file") MultipartFile file,
      @RequestParam("format") String format,
      @RequestParam(value = "outputPath", required = false) String outputPath) {

    try {
      var request = ProcessingRequest.of(file, format, outputPath);

      if (request.isLocalWrite()) {
        textProcessingService.processToLocalDisk(request);
        return ResponseEntity.ok(new StatusResponse("Success", "Saved to " + outputPath));
      }

      return buildStreamingResponse(request);

    } catch (IllegalArgumentException e) {
      log.warn("Bad request: {}", e.getMessage());
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));

    } catch (Exception e) {
      log.error("Unhandled exception during processing", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ErrorResponse("Internal processing failure"));
    }
  }

  // ── private helpers ───────────────────────────────────────────────────────

  private ResponseEntity<StreamingResponseBody> buildStreamingResponse(ProcessingRequest request) {
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
