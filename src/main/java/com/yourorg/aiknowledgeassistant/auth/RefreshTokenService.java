package com.yourorg.aiknowledgeassistant.auth;

import com.yourorg.aiknowledgeassistant.domain.user.RefreshToken;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.repository.user.RefreshTokenRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtTokenPair;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // STORE REFRESH TOKEN (On Login)
    @Transactional
    public void store(Long userId, String refreshToken, Instant expiresAt) {

        var payload = jwtService.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        RefreshToken token = new RefreshToken(
                user,
                payload.jti(),
                expiresAt
        );

        refreshTokenRepository.save(token);
    }

    // REFRESH FLOW
    @Transactional
    public JwtTokenPair refresh(String refreshToken) {

        var payload = jwtService.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        RefreshToken storedToken = refreshTokenRepository
                .findByJtiAndRevokedFalse(payload.jti())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (storedToken.isExpired()) {
            throw new IllegalStateException("Refresh token expired");
        }

        User user = storedToken.getUser();

        // VERSION VALIDATION (imp)
        if (payload.userId() == null || !payload.userId().equals(user.getId())) {
            throw new IllegalStateException("Token user mismatch");
        }

        // revoke old refresh token
        storedToken.revoke();

        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        JwtTokenPair newTokens = jwtService.generateTokens(
                user.getId(),
                user.getEmail(),
                roles,
                user.getTokenVersion()
        );

        var newPayload = jwtService.parseRefreshToken(newTokens.refreshToken())
                .orElseThrow();

        RefreshToken newRefresh = new RefreshToken(
                user,
                newPayload.jti(),
                Instant.now().plusSeconds(jwtService.getRefreshTokenTtl())
        );

        refreshTokenRepository.save(newRefresh);

        return newTokens;
    }

    // LOGOUT SINGLE DEVICE
    @Transactional
    public void logout(String refreshToken) {

        jwtService.parseRefreshToken(refreshToken)
                .flatMap(payload ->
                        refreshTokenRepository.findByJtiAndRevokedFalse(payload.jti())
                )
                .ifPresent(RefreshToken::revoke);
    }

    // LOGOUT ALL DEVICES
    @Transactional
    public void logoutAll(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Invalidate all access tokens
        user.incrementTokenVersion();

        // Revoke all refresh tokens (recommended) //AI fix
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}