package com.toollix.auth.service;

import com.toollix.auth.model.UserSession;
import com.toollix.auth.model.VerificationToken;
import com.toollix.auth.repo.UserSessionRepository;
import com.toollix.auth.repo.VerificationTokenRepository;
import com.toollix.common.audit.AuditService;
import com.toollix.common.mail.EmailService;
import com.toollix.common.security.JwtTokenProvider;
import com.toollix.users.model.User;
import com.toollix.users.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all authentication lifecycle operations.
 *
 * Role policy:
 * - Registration creates a plain User with NO role field.
 * - Roles (org-level: MEMBER, ADMIN, OWNER) are assigned separately via
 * OrganizationMember.
 * - Platform roles (SUPER_ADMIN, etc.) are set directly on the User entity by
 * ops, never via /auth.
 * - The JWT access token carries only { user_id, token_version } — roles are
 * resolved at request time.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationRepo;
    private final EmailService emailService;
    private final AuditService auditService;
    private final long refreshTokenSeconds;
    private final long verificationSeconds;

    public AuthService(JwtTokenProvider jwtTokenProvider, UserSessionRepository sessionRepo,
            UserRepository userRepo, PasswordEncoder passwordEncoder,
            VerificationTokenRepository verificationRepo, EmailService emailService,
            AuditService auditService,
            @Value("${security.jwt.refresh-token-seconds:2592000}") long refreshTokenSeconds,
            @Value("${auth.verification-seconds:86400}") long verificationSeconds) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.verificationRepo = verificationRepo;
        this.emailService = emailService;
        this.auditService = auditService;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.verificationSeconds = verificationSeconds;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registration — no role assignment here
    // ─────────────────────────────────────────────────────────────────────────

    public User register(String email, String password) {
        log.info("[AUTH] register — email={}", email);

        if (userRepo.findByEmail(email).isPresent()) {
            log.warn("[AUTH] register — email already taken: {}", email);
            throw new IllegalArgumentException("email_taken");
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setStatus("ACTIVE");
        userRepo.save(u);
        log.info("[AUTH] register — user created: id={} email={}", u.getId(), email);

        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken(u.getId(), token, Instant.now().plusSeconds(verificationSeconds));
        verificationRepo.save(vt);
        log.debug("[AUTH] register — verification token persisted for userId={}", u.getId());

        try {
            emailService.sendVerification(email, token);
            log.info("[AUTH] register — verification email dispatched to {}", email);
        } catch (Exception e) {
            // Non-fatal: log error but don't block registration;
            // the user can request a resend later.
            log.error("[AUTH] register — failed to send verification email to {}: {}", email, e.getMessage(), e);
        }

        try {
            auditService.record("user.register", String.valueOf(u.getId()), "email=" + email);
        } catch (Exception e) {
            log.warn("[AUDIT] Failed to write audit record for user.register id={}: {}", u.getId(), e.getMessage());
        }

        return u;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email verification
    // ─────────────────────────────────────────────────────────────────────────

    public void verify(String token) {
        log.info("[AUTH] verify — token={}", token.substring(0, Math.min(8, token.length())) + "...");

        VerificationToken vt = verificationRepo.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("[AUTH] verify — invalid or unknown token");
                    return new IllegalArgumentException("invalid_token");
                });

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            log.warn("[AUTH] verify — token expired for userId={}", vt.getUserId());
            throw new IllegalArgumentException("token_expired");
        }

        User user = userRepo.findById(vt.getUserId())
                .orElseThrow(() -> {
                    log.error("[AUTH] verify — userId={} not found (data integrity issue)", vt.getUserId());
                    return new IllegalArgumentException("user_not_found");
                });

        user.setStatus("ACTIVE");
        userRepo.save(user);
        verificationRepo.delete(vt);
        log.info("[AUTH] verify — userId={} activated", user.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────────

    public LoginResult login(String email, String password, String deviceName, String ipAddress, String location) {
        log.info("[AUTH] login — email={} device='{}' ip={}", email, deviceName, ipAddress);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AUTH] login — unknown email: {}", email);
                    return new IllegalArgumentException("invalid_credentials");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("[AUTH] login — wrong password for email={}", email);
            throw new IllegalArgumentException("invalid_credentials");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            log.warn("[AUTH] login — account not active: email={} status={}", email, user.getStatus());
            throw new IllegalArgumentException("unverified_account");
        }

        Long userId = user.getId();
        String access = jwtTokenProvider.createAccessToken(String.valueOf(userId), "v1", user.getEmail());

        String refresh = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(refresh);
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenSeconds);

        UserSession s = new UserSession(userId, tokenHash, expiresAt);
        s.setDeviceName(deviceName);
        s.setIpAddress(ipAddress);
        s.setLocation(location);
        sessionRepo.save(s);

        log.info("[AUTH] login — session created for userId={} sessionId={}", userId, s.getId());

        try {
            auditService.record("user.login", String.valueOf(userId), "ip=" + ipAddress + " device=" + deviceName);
        } catch (Exception e) {
            log.warn("[AUDIT] Failed to write audit record for user.login id={}: {}", userId, e.getMessage());
        }

        return new LoginResult(access, refresh);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token refresh — rotates refresh token on every use
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<LoginResult> refresh(String presentedRefresh) {
        log.debug("[AUTH] refresh — verifying presented refresh token");

        String h = sha256Hex(presentedRefresh);
        Optional<UserSession> found = sessionRepo.findByTokenHash(h);

        if (found.isEmpty()) {
            log.warn("[AUTH] refresh — token hash not found (possibly already rotated or forged)");
            return Optional.empty();
        }

        UserSession session = found.get();

        if (session.isRevoked()) {
            log.warn("[AUTH] refresh — token is revoked; possible reuse attack. sessionId={} userId={}",
                    session.getId(), session.getUserId());
            return Optional.empty();
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            log.info("[AUTH] refresh — token expired for userId={}", session.getUserId());
            return Optional.empty();
        }

        // Rotate: revoke old, issue new
        session.setRevoked(true);
        sessionRepo.save(session);
        log.debug("[AUTH] refresh — old session revoked, issuing new pair. userId={}", session.getUserId());

        Long userId = session.getUserId();
        User user = userRepo.findById(userId).orElse(null);
        String userEmail = user != null ? user.getEmail() : "user@toollix.app";

        String newAccess = jwtTokenProvider.createAccessToken(String.valueOf(userId), "v1", userEmail);
        String newRefresh = UUID.randomUUID().toString();
        String newHash = sha256Hex(newRefresh);

        UserSession ns = new UserSession(userId, newHash, Instant.now().plusSeconds(refreshTokenSeconds));
        ns.setDeviceName(session.getDeviceName());
        ns.setIpAddress(session.getIpAddress());
        ns.setLocation(session.getLocation());
        sessionRepo.save(ns);

        log.info("[AUTH] refresh — new session created: userId={} newSessionId={}", userId, ns.getId());
        return Optional.of(new LoginResult(newAccess, newRefresh));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session management
    // ─────────────────────────────────────────────────────────────────────────

    public List<UserSession> listSessions(Long userId) {
        log.debug("[AUTH] listSessions — userId={}", userId);
        return sessionRepo.findByUserIdAndRevokedFalse(userId);
    }

    public void revokeSession(UUID sessionId, Long requestingUserId) {
        log.info("[AUTH] revokeSession — sessionId={} requestedBy={}", sessionId, requestingUserId);

        sessionRepo.findById(sessionId).ifPresentOrElse(s -> {
            if (!s.getUserId().equals(requestingUserId)) {
                log.warn("[AUTH] revokeSession — userId={} attempted to revoke session owned by userId={}",
                        requestingUserId, s.getUserId());
                throw new IllegalArgumentException("session_not_owned");
            }
            s.setRevoked(true);
            sessionRepo.save(s);
            log.info("[AUTH] revokeSession — sessionId={} revoked", sessionId);
        }, () -> log.warn("[AUTH] revokeSession — sessionId={} not found", sessionId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            log.error("[AUTH] SHA-256 digest failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static record LoginResult(String accessToken, String refreshToken) {
    }
}
