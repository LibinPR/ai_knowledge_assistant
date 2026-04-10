package com.yourorg.aiknowledgeassistant.service.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        String issuer
) {}

