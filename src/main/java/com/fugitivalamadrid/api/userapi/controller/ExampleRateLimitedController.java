package com.fugitivalamadrid.api.userapi.controller;

import com.fugitivalamadrid.api.userapi.ratelimit.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Example controller demonstrating rate limiting usage.
 * Shows different rate limit configurations for various endpoints.
 */
@RestController
@RequestMapping("/api/example")
public class ExampleRateLimitedController {
    
    private static final Logger log = LoggerFactory.getLogger(ExampleRateLimitedController.class);
    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";
    private static final String PROCESSED = "processed";
    /**
     * Endpoint with moderate rate limiting - 10 requests per minute.
     */
    @GetMapping("/public-data")
    @RateLimit
    public ResponseEntity<Map<String, Object>> getPublicData() {
        log.info("Accessing public data endpoint");
        Map<String, Object> response = new HashMap<>();
        response.put(MESSAGE, "This is public data with rate limiting");
        response.put(TIMESTAMP, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint with strict rate limiting - 5 requests per minute.
     */
    @PostMapping("/expensive-operation")
    @RateLimit
    public ResponseEntity<Map<String, Object>> performExpensiveOperation(@RequestBody Map<String, Object> request) {
        log.info("Performing expensive operation");
        Map<String, Object> response = new HashMap<>();
        response.put(MESSAGE, "Expensive operation completed");
        response.put(PROCESSED, request);
        response.put(TIMESTAMP, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint with very strict rate limiting - 2 requests per minute.
     */
    @GetMapping("/premium-data")
    @RateLimit
    public ResponseEntity<Map<String, Object>> getPremiumData() {
        log.info("Accessing premium data endpoint");
        Map<String, Object> response = new HashMap<>();
        response.put(MESSAGE, "This is premium data with strict rate limiting");
        response.put("data", "Premium content here");
        response.put(TIMESTAMP, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint with custom rate limit key - separate from other endpoints.
     */
    @GetMapping("/custom-key")
    @RateLimit
    public ResponseEntity<Map<String, Object>> getCustomKeyData() {
        log.info("Accessing custom key endpoint");
        Map<String, Object> response = new HashMap<>();
        response.put(MESSAGE, "This endpoint uses a custom rate limit key");
        response.put(TIMESTAMP, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint with very fast rate limiting - 100 requests per second.
     */
    @GetMapping("/high-frequency")
    @RateLimit(maxRequests = 100, windowSizeMillis = 1000)
    public ResponseEntity<Map<String, Object>> getHighFrequencyData() {
        log.info("Accessing high frequency endpoint");
        Map<String, Object> response = new HashMap<>();
        response.put(MESSAGE, "This endpoint allows high frequency access");
        response.put(TIMESTAMP, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
