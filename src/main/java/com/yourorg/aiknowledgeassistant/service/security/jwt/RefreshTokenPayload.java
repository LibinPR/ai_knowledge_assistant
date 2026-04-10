package com.yourorg.aiknowledgeassistant.service.security.jwt;

public record RefreshTokenPayload(
        Long userId,
        String jti
) {
}
