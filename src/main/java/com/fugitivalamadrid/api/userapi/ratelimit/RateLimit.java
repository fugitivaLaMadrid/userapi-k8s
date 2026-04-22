package com.fugitivalamadrid.api.userapi.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to methods or classes.
 * Uses a simple rate limiter with AtomicInteger and synchronized blocks
 * to simulate production constraints.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Maximum number of requests allowed in the time window.
     * Default is 10 requests per minute.
     *
     * @return maximum requests
     */
    int maxRequests() default 10;
    
    /**
     * Size of the time window in milliseconds.
     * Default is 60000 milliseconds (1 minute).
     *
     * @return window size in milliseconds
     */
    long windowSizeMillis() default 60000;
    
    /**
     * Key to use for rate limiting. If not specified, uses method signature.
     * This allows different endpoints to have separate rate limits.
     *
     * @return rate limit key
     */
    String key() default "";
}
