package com.toollix.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central error handler for every controller in the application.
 *
 * All uncaught exceptions are caught here, logged at the appropriate level,
 * and mapped to a consistent JSON error envelope:
 *
 *   { "status": 400, "error": "email_taken", "path": "/auth/register", "timestamp": "..." }
 *
 * Design rules:
 *  - 4xx errors → WARN log (client mistake, not our bug)
 *  - 5xx errors → ERROR log with full stack trace
 *  - We NEVER expose internal stack traces or raw Java exception messages to callers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─────────────────────────────────────────────────────────────────────────
    // Business / domain errors – use ApiException for known, named error codes
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex, HttpServletRequest req) {
        log.warn("[ERROR] ApiException — status={} code={} path={}",
                ex.getStatus(), ex.getMessage(), req.getRequestURI());
        return ResponseEntity
                .status(ex.getStatus())
                .body(errorBody(ex.getStatus(), ex.getMessage(), req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input validation — Bean Validation (@Valid / @Validated)
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[VALIDATION] Constraint failed — fields='{}' path={}", fields, req.getRequestURI());
        Map<String, Object> body = errorBody(400, "validation_failed", req);
        body.put("fields", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String msg = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("[VALIDATION] ConstraintViolation — '{}' path={}", msg, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(400, "validation_failed", req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bad argument (legacy usage inside services — prefer ApiException going forward)
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("[BAD_REQUEST] IllegalArgument — reason='{}' path={}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, ex.getMessage(), req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authorization failures thrown manually in services
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("[FORBIDDEN] AccessDenied — reason='{}' path={}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(403, "access_denied", req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Catch-all – unknown / unhandled runtime exceptions
    // Always ERROR-level with full stack
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("[UNHANDLED] Unexpected exception at {} — {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "internal_error", req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> errorBody(int status, String error, HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("error", error);
        m.put("path", req.getRequestURI());
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    /** Convenience exception for programmatic 403 throws in service layer. */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String reason) { super(reason); }
    }
}
