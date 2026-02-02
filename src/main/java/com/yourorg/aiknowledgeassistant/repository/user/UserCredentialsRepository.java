package com.yourorg.aiknowledgeassistant.repository.user;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.user.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials , Long> {
    Optional<UserCredentials> findByUser(User user);
}
