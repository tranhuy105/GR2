package com.tranhuy105.server.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tranhuy105.server.dto.OptimizationResponse;
import com.tranhuy105.server.service.OptimizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for optimization endpoints
 */
@RestController
@RequestMapping("/api/v1/optimize")
@RequiredArgsConstructor
@Slf4j
public class OptimizationController {
    private final OptimizationService optimizationService;

    /**
     * Optimize from uploaded instance file (Schneider format txt)
     * 
     * curl -X POST -F "file=@instance.txt" "http://localhost:8080/api/v1/optimize/file?parallel=true"
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OptimizationResponse> optimizeFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "iterations", required = false) Integer iterations,
            @RequestParam(value = "timeLimit", required = false) Double timeLimit,
            @RequestParam(value = "parallel", required = false, defaultValue = "false") Boolean parallel
    ) {
        log.info("POST /api/v1/optimize/file - file: {}, parallel: {}", file.getOriginalFilename(), parallel);
        
        OptimizationResponse response = optimizationService.optimizeFromFile(file, iterations, timeLimit, parallel);
        return ResponseEntity.ok(response);
    }

    /**
     * Optimize from raw instance content (for testing/debugging)
     * 
     * curl -X POST -H "Content-Type: text/plain" -d @instance.txt "http://localhost:8080/api/v1/optimize/raw?parallel=true"
     */
    @PostMapping(value = "/raw", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<OptimizationResponse> optimizeFromRaw(
            @RequestBody String content,
            @RequestParam(value = "iterations", required = false) Integer iterations,
            @RequestParam(value = "timeLimit", required = false) Double timeLimit,
            @RequestParam(value = "parallel", required = false, defaultValue = "false") Boolean parallel
    ) {
        log.info("POST /api/v1/optimize/raw - content length: {}, parallel: {}", content.length(), parallel);
        
        OptimizationResponse response = optimizationService.optimizeFromString(content, iterations, timeLimit, parallel);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

