# ADR 002: Rate Limiting with Bucket4j

## Status
Accepted

## Context
The User API needs rate limiting to:
- Prevent abuse and DDoS attacks
- Protect backend resources (PostgreSQL)
- Ensure fair usage across clients
- Maintain API availability under load

### Requirements
- Per-client rate limiting (by IP or API key)
- Configurable limits per endpoint
- Low latency overhead (<5ms)
- In-memory storage (no external dependency for simple deployment)
- Integration with Spring Boot

## Decision
Use **Bucket4j** with Caffeine cache for in-memory rate limiting.

### Why Bucket4j

| Criteria | Bucket4j + Caffeine | Redis Rate Limiting | Spring Cloud Gateway | Custom Implementation |
|----------|---------------------|---------------------|---------------------|----------------------|
| **Token Bucket Algorithm** | ✅ Native | ✅ Lua scripts | ✅ Built-in | ⚠️ Manual |
| **Spring Integration** | ✅ @RateLimit annotation | ⚠️ Custom code | ✅ Gateway filter | ❌ Full custom |
| **Latency** | ✅ <1ms (in-memory) | ⚠️ ~5-10ms (network) | ✅ In-memory | Depends |
| **Distributed** | ❌ Single instance only | ✅ Cluster support | ✅ Cluster | ⚠️ Complex |
| **Dependencies** | ✅ Just library | ❌ Redis required | ❌ Gateway required | None |
| **Configurability** | ✅ Per-endpoint via annotation | ✅ Lua flexibility | ✅ Config file | Full control |

### Architecture

```
┌─────────────┐         ┌─────────────────────────────────┐
│   Client    │ ────────▶ │         User API                │
│  Request    │           │  ┌─────────────────────────────┐  │
│             │           │  │  @RateLimit(maxRequests=5)  │  │
└─────────────┘           │  │         │                   │  │
                          │  │         ▼                   │  │
                          │  │  ┌──────────────┐           │  │
                          │  │  │ Bucket4j     │           │  │
                          │  │  │ Token Bucket │           │  │
                          │  │  └──────────────┘           │  │
                          │  │         │                   │  │
                          │  │    Allowed?                 │  │
                          │  │    / Denied (429)           │  │
                          │  └─────────────────────────────┘  │
                          └─────────────────────────────────┘
                                        │
                                        ▼
                          ┌─────────────────────────────────┐
                          │         Controller              │
                          │      (Business Logic)           │
                          └─────────────────────────────────┘
```

### Implementation

**Annotation-based approach:**
```java
@RateLimit(maxRequests = 5, windowSizeMillis = 80000)  // 5 requests per 80s
@PostMapping
public UserResponse createUser(@RequestBody UserRequest request) {
    return userService.createUser(request);
}
```

**Storage:** Caffeine in-memory cache with 1-hour expiration

## Consequences

### Positive
- **Annotation-driven**: Clean, declarative rate limiting
- **Low overhead**: In-memory, no network calls
- **Standard algorithm**: Token bucket is industry standard
- **Flexible**: Different limits per endpoint (GET vs POST)
- **No extra infrastructure**: Works with existing app

### Negative
- **Not distributed**: Limits are per-instance (issue if scaled horizontally)
- **Memory usage**: Buckets stored in heap (mitigated by Caffeine eviction)
- **No persistence**: Limits reset on app restart
- **Client identification**: IP-based only (no API key support yet)

### Future Considerations
- If horizontal scaling needed: migrate to Redis-based rate limiting
- If API keys introduced: enhance key extractor to support header-based keys

## Alternatives Considered

### Option A: Redis Rate Limiting
- Use Redis with Lua scripts for atomic token bucket
- **Rejected**: Adds Redis dependency for MVP; acceptable trade-off for single-instance deployment

### Option B: Spring Cloud Gateway
- Rate limit at API Gateway layer
- **Rejected**: No gateway in current architecture; adds complexity

### Option C: Resilience4j Rate Limiter
- Spring ecosystem integration
- **Rejected**: Bucket4j has better token bucket implementation and cleaner Spring AOP integration

## References
- [Bucket4j Documentation](https://bucket4j.com/)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- [Rate Limiting Best Practices](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)

## Date
2026-04-15

## Author
API Development Team
