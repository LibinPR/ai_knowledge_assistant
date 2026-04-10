package com.yourorg.aiknowledgeassistant.auth;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.user.UserCredentials;
import com.yourorg.aiknowledgeassistant.repository.user.UserCredentialsRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.service.audit.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthenticationService(
            UserRepository userRepository,
            UserCredentialsRepository userCredentialsRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public AuthenticationResult authenticate(String email , String rawPassword) {
        return userRepository.findByEmail(email)
                .map(user -> authenticateUser(user, rawPassword))
                .orElseGet(() -> new AuthenticationResult.Failure("Invalid credentials"));
    }

    private AuthenticationResult authenticateUser(User user , String rawPassword) {
        if(!user.canAuthenticate()) {
            return new AuthenticationResult.Failure("User is disabled or locked");
        }

        UserCredentials credentials = userCredentialsRepository.
                findByUser(user)
                .orElseThrow();

        if(!credentials.matchesPassword(rawPassword, passwordEncoder)) {
            return new AuthenticationResult.Failure("Invalid Credentials");
        }

        auditService.log(
                user.getId(),
                "USER_AUTHENTICATED",
                "User",
                user.getId(),
                null
        );
        return new AuthenticationResult.Success(
                user.getId(),
                user.getEmail(),
                user.rolesNames(),
                user.getTokenVersion()
        );
    }
}
