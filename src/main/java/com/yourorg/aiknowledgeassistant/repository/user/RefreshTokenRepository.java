package com.yourorg.aiknowledgeassistant.repository.user;

import com.yourorg.aiknowledgeassistant.domain.user.RefreshToken;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.service.security.jwt.RefreshTokenPayload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findByUserAndRevokedFalse(User user);
    Optional<RefreshToken> findByJtiAndRevokedFalse(String jti);
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(Long userId);
}
