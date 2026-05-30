// auth-service/src/main/java/com/jiraclone/auth/repository/RefreshTokenRepository.java
package com.jiraclone.auth.repository;

import com.jiraclone.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    void deleteByUserId(UUID userId);
}
