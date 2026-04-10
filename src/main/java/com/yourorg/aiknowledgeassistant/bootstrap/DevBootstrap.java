package com.yourorg.aiknowledgeassistant.bootstrap;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.service.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevBootstrap implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    public DevBootstrap(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if(userRepository.existsByEmail("admin@local.dev")){
            return;
        }
        Long adminId = userService.registerUser(
                "admin@local.dev",
                "Libin@010"
        );
        userService.assignRole(adminId, "ADMIN");
    }
}
