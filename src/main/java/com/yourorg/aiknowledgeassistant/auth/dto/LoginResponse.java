package com.yourorg.aiknowledgeassistant.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
