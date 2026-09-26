package com.toollix.auth.repo;

import com.toollix.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByTokenHash(String tokenHash);
    List<UserSession> findByUserIdAndRevokedFalse(Long userId);
}
