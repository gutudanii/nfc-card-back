package com.toollix.auth.controller;

import com.toollix.auth.service.AuthService;
import com.toollix.auth.service.AuthService.LoginResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (req.email == null || req.email.isBlank()) {
            throw new IllegalArgumentException("email_required");
        }
        if (req.password == null || req.password.length() < 8) {
            throw new IllegalArgumentException("password_too_short");
        }
        var u = authService.register(req.email, req.password);
        return ResponseEntity.ok(java.util.Map.of("userId", u.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        if (req.email == null || req.email.isBlank()) {
            throw new IllegalArgumentException("email_required");
        }
        if (req.password == null || req.password.isBlank()) {
            throw new IllegalArgumentException("password_required");
        }
        LoginResult r = authService.login(req.email, req.password);
        return ResponseEntity.ok(new LoginResponse(r.accessToken(), r.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        if (req.refreshToken == null || req.refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        var maybe = authService.refresh(req.refreshToken);
        return maybe.map(r -> ResponseEntity.ok(new LoginResponse(r.accessToken(), r.refreshToken())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
        if (req.token == null || req.token.isBlank()) {
            throw new IllegalArgumentException("token_required");
        }
        authService.verify(req.token);
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionDto>> sessions(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        var list = authService.listSessions(userId);
        var dto = list.stream().map(s ->
                new SessionDto(s.getId(), s.getDeviceName(), s.isRevoked())).toList();
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revoke(@PathVariable UUID sessionId) {
        authService.revokeSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    public static record RegisterRequest(String email, String password) {}
    public static record LoginRequest(String email, String password) {}
    public static record LoginResponse(String accessToken, String refreshToken) {}
    public static record RefreshRequest(String refreshToken) {}
    public static record VerifyRequest(String token) {}
    public static record SessionDto(java.util.UUID id, String deviceName, boolean revoked) {}
}
