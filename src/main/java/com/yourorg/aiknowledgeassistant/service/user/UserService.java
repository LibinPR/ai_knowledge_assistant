package com.yourorg.aiknowledgeassistant.service.user;

import com.yourorg.aiknowledgeassistant.domain.user.Role;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.user.UserCredentials;
import com.yourorg.aiknowledgeassistant.domain.user.UserRole;
import com.yourorg.aiknowledgeassistant.dto.DocumentSummaryResponse;
import com.yourorg.aiknowledgeassistant.exception.EmailAlreadyExistsException;
import com.yourorg.aiknowledgeassistant.repository.user.RoleRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserCredentialsRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRoleRepository;
import com.yourorg.aiknowledgeassistant.service.audit.AuditService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;
import com.yourorg.aiknowledgeassistant.dto.DocumentSummaryResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final DocumentRepository documentRepository;

    public UserService(
            UserRepository userRepository,
            UserCredentialsRepository userCredentialsRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            DocumentRepository documentRepository
    ) {
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public Long registerUser(String email , String rawPassword) {

        if(userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already Registered");
        }

        User user = new User(email);
        userRepository.saveAndFlush(user);

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
        return user.getId();
    }

    @Transactional
    public void assignRole(Long userId , String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow();

        Role role = roleRepository.findByName(roleName)
                .orElseThrow();

        user.addRole(role);
    }

    public List<DocumentSummaryResponse> listDocuments(
        Long userId
    ) {
        return documentRepository
            .findByUploadedById(userId)
            .stream()
            .map(doc -> new DocumentSummaryResponse(doc.getId(), doc.getFilename()))
            .toList();
    }
}
