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

@Service
public class AuthService {

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

    public User register(String email, String password) {
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("email_taken");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setStatus("UNVERIFIED");
        userRepo.save(u);

        // create verification token and send email
        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken(u.getId(), token, Instant.now().plusSeconds(verificationSeconds));
        verificationRepo.save(vt);
        emailService.sendVerification(email, token);

        // audit
        try { auditService.record("user.register", String.valueOf(u.getId()), "email="+email); } catch (Exception ignored) {}

        return u;
    }

    public void verify(String token) {
        var opt = verificationRepo.findByToken(token);
        var vt = opt.orElseThrow(() -> new IllegalArgumentException("invalid_token"));
        if (vt.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("token_expired");
        var user = userRepo.findById(vt.getUserId()).orElseThrow(() -> new IllegalArgumentException("user_not_found"));
        user.setStatus("ACTIVE");
        userRepo.save(user);
        verificationRepo.delete(vt);
    }

    public LoginResult login(String email, String password) {
        User user = userRepo.findByEmail(email).orElseThrow(()
                -> new IllegalArgumentException("invalid_credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid_credentials");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("unverified_account");
        }

        Long userId = user.getId();
        String access = jwtTokenProvider.createAccessToken(String.valueOf(userId), "v1");
        String refresh = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(refresh);
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenSeconds);
        UserSession s = new UserSession(userId, tokenHash, expiresAt);
        sessionRepo.save(s);

        return new LoginResult(access, refresh);
    }

    public Optional<LoginResult> refresh(String presentedRefresh) {
        String h = sha256Hex(presentedRefresh);
        Optional<UserSession> found = sessionRepo.findByTokenHash(h);
        if (found.isEmpty()) return Optional.empty();
        UserSession session = found.get();
        if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())) return Optional.empty();

        // rotate: revoke old, create new session
        session.setRevoked(true);
        sessionRepo.save(session);

        Long userId = session.getUserId();
        String newAccess = jwtTokenProvider.createAccessToken(String.valueOf(userId), "v1");
        String newRefresh = UUID.randomUUID().toString();
        String newHash = sha256Hex(newRefresh);
        UserSession ns = new UserSession(userId, newHash, Instant.now().plusSeconds(refreshTokenSeconds));
        sessionRepo.save(ns);

        return Optional.of(new LoginResult(newAccess, newRefresh));
    }

    public List<UserSession> listSessions(Long userId) {
        return sessionRepo.findByUserIdAndRevokedFalse(userId);
    }

    public void revokeSession(java.util.UUID sessionId) {
        sessionRepo.findById(sessionId).ifPresent(s -> { s.setRevoked(true); sessionRepo.save(s); });
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static record LoginResult(String accessToken, String refreshToken) {}
}
