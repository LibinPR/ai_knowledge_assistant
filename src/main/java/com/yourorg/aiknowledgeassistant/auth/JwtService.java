package com.yourorg.aiknowledgeassistant.auth;

import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtPayload;
import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtProperties;
import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtTokenPair;
import com.yourorg.aiknowledgeassistant.service.security.jwt.RefreshTokenPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties props;

    @Value("${security.jwt.refresh-token-ttl-seconds}")
    private long refreshTokenTtl;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = Decoders.BASE64.decode(props.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // ACCESS TOKEN
    public String generateAccessToken(
            Long userId,
            String email,
            Set<String> roles,
            int version
    ) {
        Instant now = Instant.now();
        String accessJti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(props.issuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.accessTokenTtlSeconds())))
                .claim("email", email)
                .claim("roles", roles)
                .claim("jti", accessJti)
                .claim("type", "access")
                .claim("version", version)
                .signWith(key)
                .compact();
    }

    // REFRESH TOKEN
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        String refreshJti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(props.issuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.refreshTokenTtlSeconds())))
                .claim("type", "refresh")
                .claim("jti", refreshJti)
                .claim("userId", userId)
                .signWith(key)
                .compact();
    }

    // TOKEN PAIR
    public JwtTokenPair generateTokens(
            Long userId,
            String email,
            Set<String> roles,
            int version
    ) {
        return new JwtTokenPair(
                generateAccessToken(userId, email, roles, version),
                generateRefreshToken(userId)
        );
    }

    // PARSE ACCESS TOKEN
    public Optional<JwtPayload> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                return Optional.empty();
            }

            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);

            Collection<?> rolesClaim = claims.get("roles", Collection.class);
            Set<String> roles = rolesClaim == null
                    ? Set.of()
                    : rolesClaim.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());

            String jti = claims.get("jti", String.class);

            Integer version = claims.get("version", Integer.class);
            if (version == null) {
                return Optional.empty();
            }

            return Optional.of(
                    new JwtPayload(userId, email, Set.copyOf(roles), jti, version)
            );

        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    // PARSE REFRESH TOKEN
    public Optional<RefreshTokenPayload> parseRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                return Optional.empty();
            }

            Long userId = claims.get("userId", Long.class);
            String jti = claims.get("jti", String.class);

            if (userId == null || jti == null) {
                return Optional.empty();
            }

            return Optional.of(
                    new RefreshTokenPayload(userId, jti)
            );

        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public long getRefreshTokenTtl() {
        return refreshTokenTtl;
    }
}