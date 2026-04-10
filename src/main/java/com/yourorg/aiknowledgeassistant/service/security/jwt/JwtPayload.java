package com.yourorg.aiknowledgeassistant.service.security.jwt;

import java.util.Set;

public record JwtPayload(
        Long userId,
        String email,
        Set<String> roles,
        String Jti,
        int version
) {
}
