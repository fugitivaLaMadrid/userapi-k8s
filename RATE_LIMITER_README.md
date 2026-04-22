# Rate Limiter Implementation

This project implements a simple rate limiter using `AtomicInteger` and `synchronized` to simulate production constraints.

## Components

### 1. RateLimiter Class
- **Location**: `src/main/java/com/fugitivalamadrid/api/userapi/ratelimit/RateLimiter.java`
- **Features**:
  - Uses `AtomicInteger` for thread-safe request counting
  - Uses `synchronized` blocks for window reset operations
  - Sliding window approach with configurable limits
  - Thread-safe for concurrent access

### 2. @RateLimit Annotation
- **Location**: `src/main/java/com/fugitivalamadrid/api/userapi/ratelimit/RateLimit.java`
- **Usage**: Apply to methods or classes to enable rate limiting
- **Parameters**:
  - `maxRequests`: Maximum requests per window (default: 10)
  - `windowSizeMillis`: Window size in milliseconds (default: 60000)
  - `key`: Custom rate limit key (optional)

### 3. RateLimitAspect
- **Location**: `src/main/java/com/fugitivalamadrid/api/userapi/ratelimit/RateLimitAspect.java`
- **Functionality**: Intercepts methods annotated with `@RateLimit` and enforces limits

### 4. RateLimitException
- **Location**: `src/main/java/com/fugitivalamadrid/api/userapi/exception/RateLimitException.java`
- **Purpose**: Thrown when rate limits are exceeded

### 5. RateLimitExceptionHandler
- **Location**: `src/main/java/com/fugitivalamadrid/api/userapi/exception/RateLimitExceptionHandler.java`
- **Functionality**: Handles rate limit exceptions and returns HTTP 429 responses

## Usage Examples

### Basic Usage
```java
@GetMapping("/public-data")
@RateLimit(maxRequests = 10, windowSizeMillis = 60000)
public ResponseEntity<String> getPublicData() {
    return ResponseEntity.ok("Public data");
}
```

### Custom Rate Limit Key
```java
@PostMapping("/expensive-operation")
@RateLimit(maxRequests = 5, windowSizeMillis = 60000, key = "expensive-ops")
public ResponseEntity<String> performExpensiveOperation() {
    return ResponseEntity.ok("Operation completed");
}
```

### Different Rate Limits per Endpoint
```java
// Strict rate limiting for premium features
@GetMapping("/premium")
@RateLimit(maxRequests = 2, windowSizeMillis = 60000)
public ResponseEntity<String> getPremiumData() {
    return ResponseEntity.ok("Premium content");
}

// High frequency for public APIs
@GetMapping("/high-frequency")
@RateLimit(maxRequests = 100, windowSizeMillis = 1000)
public ResponseEntity<String> getHighFrequencyData() {
    return ResponseEntity.ok("High frequency data");
}
```

## Testing

### Run Unit Tests
```bash
mvn test -Dtest=RateLimiterTest
```

### Test Coverage
The tests cover:
- Basic rate limiting functionality
- Window reset behavior
- Concurrent access safety
- Parameter validation
- Configuration verification

## Example Endpoints

The project includes an example controller at:
`src/main/java/com/fugitivalamadrid/api/userapi/controller/ExampleRateLimitedController.java`

Test endpoints:
- `GET /api/example/public-data` - 10 requests/minute
- `POST /api/example/expensive-operation` - 5 requests/minute
- `GET /api/example/premium-data` - 2 requests/minute
- `GET /api/example/custom-key` - 15 requests/minute (separate key)
- `GET /api/example/high-frequency` - 100 requests/second

## Applying to UserController

To apply rate limiting to the existing UserController, add the import and annotations:

```java
import com.fugitivalamadrid.api.userapi.ratelimit.RateLimit;

@RestController
@RequestMapping("/users")
public class UserController {
    
    @GetMapping
    @RateLimit(maxRequests = 20, windowSizeMillis = 60000)
    public List<UserResponse> getAllUsers() {
        // implementation
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(maxRequests = 5, windowSizeMillis = 60000)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        // implementation
    }
}
```

## Rate Limit Response Format

When rate limits are exceeded, the API returns:
```json
{
  "timestamp": "2023-12-07T10:30:45.123",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 10 requests per 60000 ms allowed. Try again in 15432 ms.",
  "maxRequests": 10,
  "windowSizeMillis": 60000,
  "timeUntilReset": 15432
}
```

## Production Considerations

This implementation uses simple in-memory rate limiting. For production:
1. Consider distributed rate limiting for multiple instances
2. Add monitoring and metrics for rate limit violations
3. Implement different rate limits per user/API key
4. Consider using Redis or other distributed stores for rate limit state

## Performance Characteristics

- **Thread Safety**: Uses `AtomicInteger` and `synchronized` blocks
- **Memory Usage**: Minimal - one `RateLimiter` per unique key
- **Overhead**: Low - atomic operations and minimal synchronization
- **Accuracy**: Window-based with potential slight variations in timing
