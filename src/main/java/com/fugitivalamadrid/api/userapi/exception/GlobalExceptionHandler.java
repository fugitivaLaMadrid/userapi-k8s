package com.fugitivalamadrid.api.userapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // Response field constants
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String STATUS_KEY = "status";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    // HTTP Status codes
    private static final int BAD_REQUEST_STATUS = 400;
    private static final int NOT_FOUND_STATUS = 404;
    private static final int CONFLICT_STATUS = 409;
    private static final int TOO_MANY_REQUESTS_STATUS = 429;

    // Error messages
    private static final String NOT_FOUND_ERROR = "Not Found";
    private static final String CONFLICT_ERROR = "Conflict";
    private static final String TOO_MANY_REQUESTS_ERROR = "Too Many Requests";
    private static final String DUPLICATE_USER_MESSAGE = "A user with this email or username already exists";
    private static final String BAD_REQUEST_ERROR = "Bad Request";
    /**
     * Handle User Not Found Exception
     */
    @SuppressWarnings("unused")
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, NOT_FOUND_STATUS,
                ERROR_KEY, NOT_FOUND_ERROR,
                MESSAGE_KEY, ex.getMessage()
        ));
    }

    /**
     * Handle Duplicate Key Exception (e.g., email or username already exists)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKey(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, CONFLICT_STATUS,
                ERROR_KEY, CONFLICT_ERROR,
                MESSAGE_KEY, DUPLICATE_USER_MESSAGE
        ));
    }

    /**
     * Handle Rate Limit Exception
     */
    @SuppressWarnings("unused")
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, TOO_MANY_REQUESTS_STATUS,
                ERROR_KEY, TOO_MANY_REQUESTS_ERROR,
                MESSAGE_KEY, ex.getMessage(),
                "maxRequests", ex.getMaxRequests(),
                "windowSizeMillis", ex.getWindowSizeMillis(),
                "timeUntilReset", ex.getTimeUntilReset()
        ));
    }

    /**
     * Handles malformed JSON body
     * Example: sending "username: 123" without quotes
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, BAD_REQUEST_STATUS,
                ERROR_KEY, BAD_REQUEST_ERROR,
                MESSAGE_KEY, "Malformed JSON request body"
        ));
    }

    /**
     * Handles validation errors from @Valid
     * Example: blank username, invalid email
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, BAD_REQUEST_STATUS,
                ERROR_KEY, BAD_REQUEST_ERROR,
                MESSAGE_KEY, message
        ));
    }

    /**
     * Handles wrong type in path variable
     * Example: GET /users/abc instead of GET /users/1
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, BAD_REQUEST_STATUS,
                ERROR_KEY, BAD_REQUEST_ERROR,
                MESSAGE_KEY, "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'"
        ));
    }

    /**
     * Handles invalid request parameters.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, BAD_REQUEST_STATUS,
                ERROR_KEY, BAD_REQUEST_ERROR,
                MESSAGE_KEY, ex.getMessage()
        ));
    }

    /**
     * Handles HTTP method not allowed
     * Example: POST /users/1 (POST is not allowed on that endpoint)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, 405,
                ERROR_KEY, "Method Not Allowed",
                MESSAGE_KEY, "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint"
        ));
    }

    /**
     * Handles endpoint not found
     * Example: GET /wrongpath
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, 404,
                ERROR_KEY, NOT_FOUND_ERROR,
                MESSAGE_KEY, "The requested endpoint does not exist"
        ));
    }

    /**
     * Catch-all for any unexpected exception
     * Prevents exposing internal error details to the client
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                TIMESTAMP_KEY, LocalDateTime.now().toString(),
                STATUS_KEY, 500,
                ERROR_KEY, "Internal Server Error",
                MESSAGE_KEY, "An unexpected error occurred"
        ));
    }
}
