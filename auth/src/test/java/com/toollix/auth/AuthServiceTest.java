package com.toollix.auth;

import com.toollix.auth.repo.VerificationTokenRepository;
import com.toollix.auth.repo.UserSessionRepository;
import com.toollix.auth.service.AuthService;
import com.toollix.users.repo.UserRepository;
import com.toollix.common.mail.EmailService;
import com.toollix.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class AuthServiceTest {

    @Test
    public void register_sendsEmail_and_persistsUser() {
        var jwt = new JwtTokenProvider("01234567890123456789012345678901", 900L);
        var sess = Mockito.mock(UserSessionRepository.class);
        var userRepo = Mockito.mock(UserRepository.class);
        var verificationRepo = Mockito.mock(VerificationTokenRepository.class);
        var email = Mockito.mock(EmailService.class);
        var encoder = new BCryptPasswordEncoder();
        var audit = new com.toollix.common.audit.AuditService(null);

<<<<<<< HEAD
        var service = new AuthService(jwt, sess, userRepo, encoder, verificationRepo, email, audit, 2592000L, 3600L);

        // when repo save called, create a dummy user with id
        Mockito.when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.empty());
        Mockito.when(userRepo.save(any())).thenAnswer(inv -> { var u = (com.toollix.users.model.User) inv.getArgument(0); u.setEmail("a@b.com"); return u; });
=======
        var jdbc = Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var service = new AuthService(jwt, sess, userRepo, encoder, verificationRepo, email, audit, 2592000L, 3600L,
                jdbc);

        // when repo save called, create a dummy user with id
        Mockito.when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.empty());
        Mockito.when(userRepo.save(any())).thenAnswer(inv -> {
            var u = (com.toollix.users.model.User) inv.getArgument(0);
            u.setEmail("a@b.com");
            return u;
        });
>>>>>>> 82aa1f0 (Initial commit)

        var u = service.register("a@b.com", "pass");
        assertNotNull(u);
        verify(email).sendVerification(Mockito.eq("a@b.com"), any());
    }
}
