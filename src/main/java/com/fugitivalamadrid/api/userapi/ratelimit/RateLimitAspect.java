package com.fugitivalamadrid.api.userapi.ratelimit;

import com.fugitivalamadrid.api.userapi.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Aspect that applies rate limiting to methods annotated with @RateLimit.
 * Uses a simple rate limiter with AtomicInteger and synchronized blocks
 * to simulate production constraints.
 */
@Aspect
@Component
public class RateLimitAspect {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    
    // Map to store rate limiters per key
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    /**
     * Applies rate limiting around methods annotated with @RateLimit.
     *
     * @param joinPoint the join point
     * @param rateLimit the rate limit annotation
     * @return the result of the method invocation
     * @throws Throwable if method invocation fails or rate limit is exceeded
     */
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable { // NOSONAR - Used by AOP
        String key = generateKey(joinPoint, rateLimit);
        RateLimiter limiter = getOrCreateRateLimiter(key, rateLimit);
        
        if (!limiter.tryAcquire()) {
            log.warn("Rate limit exceeded for key: {}. Current: {}, Max: {}", 
                    key, limiter.getCurrentRequestCount(), limiter.getMaxRequests());
            
            throw new RateLimitException(
                String.format("Rate limit exceeded. Maximum %d requests per %d ms allowed. " +
                             "Try again in %d ms.", 
                             limiter.getMaxRequests(), 
                             limiter.getWindowSizeMillis(), 
                             limiter.getTimeUntilReset()),
                limiter.getMaxRequests(),
                limiter.getWindowSizeMillis(),
                limiter.getTimeUntilReset()
            );
        }
        
        log.debug("Rate limit check passed for key: {}. Current: {}, Remaining: {}", 
                key, limiter.getCurrentRequestCount(), limiter.getRemainingRequests());
        
        return joinPoint.proceed();
    }
    
    /**
     * Generates a unique key for rate limiting based on the join point and annotation.
     *
     * @param joinPoint the join point
     * @param rateLimit the rate limit annotation
     * @return unique key for rate limiting
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimit.key().isEmpty()) {
            return rateLimit.key();
        }
        
        // Generate key from class and method name
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "." + methodName;
    }
    
    /**
     * Gets or creates a rate limiter for the given key.
     *
     * @param key the rate limit key
     * @param rateLimit the rate limit annotation
     * @return the rate limiter
     */
    private RateLimiter getOrCreateRateLimiter(String key, RateLimit rateLimit) {
        return rateLimiters.computeIfAbsent(key, k -> 
            RateLimiter.create(rateLimit.maxRequests(), rateLimit.windowSizeMillis()));
    }
}
