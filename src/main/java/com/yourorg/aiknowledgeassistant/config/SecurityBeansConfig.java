package com.yourorg.aiknowledgeassistant.config;

import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtAuthenticationFilter;
import com.yourorg.aiknowledgeassistant.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService , UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtService , userRepository);
    }
}
