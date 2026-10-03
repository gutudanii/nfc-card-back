package com.toollix.auth.controller;

import com.toollix.auth.dto.SessionDto;
import com.toollix.auth.service.AuthService;
import com.toollix.auth.service.AuthService.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication endpoints.
 *
 * Role policy reminder:
 * POST /auth/register — creates a bare User. No role is set or accepted here.
 * POST /auth/login — returns JWT carrying only user_id (no roles in token).
 * Org roles are assigned via POST /orgs/{orgId}/members after the user exists.
 * Platform admin roles are set by ops directly on the DB — never via API.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        log.info("[AUTH_CTRL] POST /auth/register — email={}", req.email());
        var u = authService.register(req.email(), req.password());
        log.info("[AUTH_CTRL] Register success — userId={}", u.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("userId", u.getId(), "status", "verification_email_sent"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify email
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest req) {
        log.info("[AUTH_CTRL] POST /auth/verify");
        authService.verify(req.token());
        return ResponseEntity.ok(Map.of("status", "verified"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq) {

        // Extract device context for session tracking (powers the Active Sessions UI)
        String deviceName = parseDeviceName(httpReq.getHeader("User-Agent"));
        String ip = getClientIp(httpReq);
        log.info("[AUTH_CTRL] POST /auth/login — email={} ip={} device='{}'", req.email(), ip, deviceName);

        LoginResult r = authService.login(req.email(), req.password(), deviceName, ip, null /*
                                                                                             * geolocation: enrich via
                                                                                             * async job
                                                                                             */);
        log.info("[AUTH_CTRL] Login success — issuing tokens");
        return ResponseEntity.ok(new LoginResponse(r.accessToken(), r.refreshToken()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        log.debug("[AUTH_CTRL] POST /auth/refresh");
        var maybe = authService.refresh(req.refreshToken());
        return maybe
                .map(r -> {
                    log.info("[AUTH_CTRL] Token refreshed successfully");
                    return ResponseEntity.ok(new LoginResponse(r.accessToken(), r.refreshToken()));
                })
                .orElseGet(() -> {
                    log.warn("[AUTH_CTRL] Token refresh failed — invalid or expired refresh token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sessions
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionDto>> sessions(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        log.info("[AUTH_CTRL] GET /auth/sessions — userId={}", userId);
        var list = authService.listSessions(userId);
        var dto = list.stream().map(s -> new SessionDto(
                s.getId(), s.getDeviceName(), s.getIpAddress(),
                s.getLocation(), s.getCreatedAt(), s.getLastUsedAt(), s.isRevoked())).toList();
        log.debug("[AUTH_CTRL] sessions for userId={} — count={}", userId, dto.size());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revoke(
            @PathVariable("sessionId") UUID sessionId,
            @AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        log.info("[AUTH_CTRL] DELETE /auth/sessions/{} — requestedBy userId={}", sessionId, userId);
        authService.revokeSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logout (revokes ALL sessions for the current user)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal String principal) {
        if (principal == null)
            return ResponseEntity.ok().build();
        Long userId = Long.parseLong(principal);
        log.info("[AUTH_CTRL] POST /auth/logout — userId={}", userId);
        authService.listSessions(userId).forEach(s -> {
            if (!s.isRevoked())
                authService.revokeSession(s.getId(), userId);
        });
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forgot / Reset password
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        log.info("[AUTH_CTRL] POST /auth/forgot-password — email={}", req.email());
        authService.forgotPassword(req.email());
        // Always return 200 to prevent email enumeration
        return ResponseEntity.ok(Map.of("status", "reset_email_sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        log.info("[AUTH_CTRL] POST /auth/reset-password");
        authService.resetPassword(req.token(), req.password());
        return ResponseEntity.ok(Map.of("status", "password_reset_successful"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Request / Response records — Bean Validation annotations are the first
    // line of defense; GlobalExceptionHandler renders them as 400 responses.
    // ─────────────────────────────────────────────────────────────────────────

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String refreshToken) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record VerifyRequest(@NotBlank String token) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String parseDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank())
            return "Unknown Device";
        if (userAgent.contains("iPhone"))
            return "iPhone";
        if (userAgent.contains("iPad"))
            return "iPad";
        if (userAgent.contains("Android"))
            return "Android Device";
        if (userAgent.contains("Macintosh"))
            return "Mac";
        if (userAgent.contains("Windows"))
            return "Windows PC";
        if (userAgent.contains("Linux"))
            return "Linux Device";
        return "Browser";
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
