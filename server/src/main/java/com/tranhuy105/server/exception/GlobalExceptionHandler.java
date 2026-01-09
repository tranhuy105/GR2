package com.tranhuy105.server.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST API
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InstanceParseException.class)
    public ResponseEntity<ErrorResponse> handleParsing(InstanceParseException ex) {
        log.error("Instance parse error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("PARSE_ERROR", List.of(ex.getMessage())));
    }

    @ExceptionHandler(OptimizationException.class)
    public ResponseEntity<ErrorResponse> handleOptimization(OptimizationException ex) {
        log.error("Optimization error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("OPTIMIZATION_ERROR", List.of(ex.getMessage())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("FILE_TOO_LARGE", List.of("Maximum file size exceeded")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_ARGUMENT", List.of(ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", List.of("An unexpected error occurred")));
    }

    public record ErrorResponse(
        String code,
        List<String> messages,
        Instant timestamp
    ) {
        public ErrorResponse(String code, List<String> messages) {
            this(code, messages, Instant.now());
        }
    }
}
