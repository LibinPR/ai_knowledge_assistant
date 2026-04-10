package com.yourorg.aiknowledgeassistant.config;

import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

}
