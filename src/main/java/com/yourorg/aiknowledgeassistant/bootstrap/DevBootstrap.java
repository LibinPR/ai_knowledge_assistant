package com.yourorg.aiknowledgeassistant.bootstrap;

import com.yourorg.aiknowledgeassistant.service.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevBootstrap implements CommandLineRunner {

    private final UserService userService;

    public DevBootstrap(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        userService.registerUser(
                "admin@local.dev",
                "Libin@010"
        );
    }
}
