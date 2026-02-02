package com.yourorg.aiknowledgeassistant.service.user;

import com.yourorg.aiknowledgeassistant.domain.user.Role;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.user.UserCredentials;
import com.yourorg.aiknowledgeassistant.domain.user.UserRole;
import com.yourorg.aiknowledgeassistant.repository.user.RoleRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserCredentialsRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRoleRepository;
import com.yourorg.aiknowledgeassistant.service.audit.AuditService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            UserCredentialsRepository userCredentialsRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public void registerUser(String email , String rawPassword) {

        if(userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already Registered");
        }

        User user = new User(email);
        userRepository.saveAndFlush(user);

        userRepository.flush();

        String hashedPassword = passwordEncoder.encode(rawPassword);
        UserCredentials credentials = new UserCredentials(user , hashedPassword);
        userCredentialsRepository.saveAndFlush(credentials);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default role USER not found"));

        UserRole assignment = new UserRole(user , userRole);
        userRoleRepository.save(assignment);

        auditService.log(
                user.getId(),
                "USER REGISTERED",
                "User",
                user.getId(),
                null
        );
    }
}
