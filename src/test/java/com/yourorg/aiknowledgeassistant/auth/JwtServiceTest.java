package com.yourorg.aiknowledgeassistant.auth;

import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;

class JwtServiceTest {
    @Test
    void acceptsUrlSafeBase64Secrets() {
        String rawSecret = "a-strong-development-secret-with-32+bytes";
        String urlSafeSecret = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawSecret.getBytes(StandardCharsets.UTF_8));

        assertThatCode(() -> new JwtService(new JwtProperties(urlSafeSecret, 900, 604800, "test")))
                .doesNotThrowAnyException();
    }
}
