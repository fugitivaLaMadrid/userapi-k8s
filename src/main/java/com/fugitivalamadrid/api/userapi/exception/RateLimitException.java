package com.fugitivalamadrid.api.userapi.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limit is exceeded.
 * This exception is used to indicate that a request has been rejected
 * due to exceeding the configured rate limit constraints.
 */
@Getter
public class RateLimitException extends RuntimeException {
    
    private final int maxRequests;
    private final long windowSizeMillis;
    private final long timeUntilReset;
    
    /**
     * Constructs a new RateLimitException with the specified details.
     *
     * @param message the detail message
     * @param maxRequests maximum number of requests allowed
     * @param windowSizeMillis size of the time window in milliseconds
     * @param timeUntilReset time until the rate limit window resets in milliseconds
     */
    public RateLimitException(String message, int maxRequests, long windowSizeMillis, long timeUntilReset) {
        super(message);
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.timeUntilReset = timeUntilReset;
    }
}
