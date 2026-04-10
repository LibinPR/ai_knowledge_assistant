package com.yourorg.aiknowledgeassistant.service.security.jwt;

    public record JwtTokenPair(
            String accessToken,
            String refreshToken
    ) {}

