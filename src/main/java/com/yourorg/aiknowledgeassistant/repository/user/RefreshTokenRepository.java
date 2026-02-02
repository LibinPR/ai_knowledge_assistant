package com.yourorg.aiknowledgeassistant.repository.user;

import com.yourorg.aiknowledgeassistant.domain.user.RefreshToken;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevokedFalse(User user);

}
