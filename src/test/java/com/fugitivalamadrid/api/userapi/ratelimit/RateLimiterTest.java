package com.fugitivalamadrid.api.userapi.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = RateLimiter.create(5, 1000); // 5 requests per second
    }

    @Test
    @DisplayName("Should allow requests within limit")
    void shouldAllowRequestsWithinLimit() {
        // Should allow 5 requests
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(), "Request " + (i + 1) + " should be allowed");
        }
        assertEquals(5, rateLimiter.getCurrentRequestCount());
        assertEquals(0, rateLimiter.getRemainingRequests());
    }

    @Test
    @DisplayName("Should reject requests exceeding limit")
    void shouldRejectRequestsExceedingLimit() {
        // Consume all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire();
        }

        // Next request should be rejected
        assertFalse(rateLimiter.tryAcquire(), "Request exceeding limit should be rejected");
        assertEquals(5, rateLimiter.getCurrentRequestCount());
        assertEquals(0, rateLimiter.getRemainingRequests());
    }

    @Test
    @DisplayName("Should reset window after time expires")
    void shouldResetWindowAfterTimeExpires() {
        // Consume all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire();
        }

        // Wait for window to expire
        try {
            Thread.sleep(1100); // Wait 1.1 seconds for window to expire
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should allow request after window reset
        assertTrue(rateLimiter.tryAcquire(), "Should allow request after window reset");
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccess() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int REQUESTS_PER_THREAD = 10;
        final int TOTAL_EXPECTED_ALLOWED = 5; // Only 5 should be allowed per window

        Thread[] threads = new Thread[THREAD_COUNT];
        int[] allowedCounts = new int[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                int allowed = 0;
                for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                    if (rateLimiter.tryAcquire()) {
                        allowed++;
                    }
                }
                allowedCounts[threadIndex] = allowed;
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify total allowed requests
        int totalAllowed = 0;
        for (int count : allowedCounts) {
            totalAllowed += count;
        }

        assertEquals(TOTAL_EXPECTED_ALLOWED, totalAllowed, 
            "Total allowed requests should not exceed the rate limit");
        assertEquals(TOTAL_EXPECTED_ALLOWED, rateLimiter.getCurrentRequestCount());
    }

    @Test
    @DisplayName("Should throw exception for invalid parameters")
    void shouldThrowExceptionForInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> 
            RateLimiter.create(0, 1000), "maxRequests must be positive");
        
        assertThrows(IllegalArgumentException.class, () -> 
            RateLimiter.create(5, 0), "windowSizeMillis must be positive");
        
        assertThrows(IllegalArgumentException.class, () -> 
            RateLimiter.create(-1, 1000), "maxRequests must be positive");
        
        assertThrows(IllegalArgumentException.class, () -> 
            RateLimiter.create(5, -1), "windowSizeMillis must be positive");
    }

    @Test
    @DisplayName("Should provide correct remaining requests")
    void shouldProvideCorrectRemainingRequests() {
        assertEquals(5, rateLimiter.getRemainingRequests());
        
        rateLimiter.tryAcquire();
        assertEquals(4, rateLimiter.getRemainingRequests());
        
        rateLimiter.tryAcquire();
        assertEquals(3, rateLimiter.getRemainingRequests());
        
        // Consume remaining
        rateLimiter.tryAcquire();
        rateLimiter.tryAcquire();
        rateLimiter.tryAcquire();
        assertEquals(0, rateLimiter.getRemainingRequests());
    }

    @Test
    @DisplayName("Should provide correct time until reset")
    void shouldProvideCorrectTimeUntilReset() {
        long timeUntilReset = rateLimiter.getTimeUntilReset();
        assertTrue(timeUntilReset > 0 && timeUntilReset <= 1000, 
            "Time until reset should be positive and within window size");
        
        // Time until reset should be less than or equal to window size
        assertTrue(timeUntilReset <= 1000, 
            "Time until reset should be within window size");
    }

    @Test
    @DisplayName("Should return correct configuration values")
    void shouldReturnCorrectConfigurationValues() {
        assertEquals(5, rateLimiter.getMaxRequests());
        assertEquals(1000, rateLimiter.getWindowSizeMillis());
    }
}
