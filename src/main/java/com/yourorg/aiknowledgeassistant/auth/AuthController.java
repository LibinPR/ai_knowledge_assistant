package com.yourorg.aiknowledgeassistant.auth;

import com.yourorg.aiknowledgeassistant.auth.dto.*;
import com.yourorg.aiknowledgeassistant.service.security.jwt.JwtTokenPair;
import com.yourorg.aiknowledgeassistant.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UserService userService,
            AuthenticationService authenticationService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        Long userId = userService.registerUser(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok("User registered with id: " + userId);
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthenticationResult result =
                authenticationService.authenticate(
                        request.email(),
                        request.password()
                );

        if (result instanceof AuthenticationResult.Failure) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        AuthenticationResult.Success success =
                (AuthenticationResult.Success) result;

        //included tokenVersion
        JwtTokenPair tokens = jwtService.generateTokens(
                success.userId(),
                success.email(),
                success.roles(),
                success.tokenVersion()
        );

        refreshTokenService.store(
                success.userId(),
                tokens.refreshToken(),
                Instant.now().plusSeconds(jwtService.getRefreshTokenTtl())
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        tokens.accessToken(),
                        tokens.refreshToken()
                )
        );
    }

    // REFRESH
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        JwtTokenPair tokens =
                refreshTokenService.refresh(request.refreshToken());

        return ResponseEntity.ok(
                new LoginResponse(
                        tokens.accessToken(),
                        tokens.refreshToken()
                )
        );
    }


    // LOGOUT (single device)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        refreshTokenService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    // LOGOUT ALL DEVICES
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        if(authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        refreshTokenService.logoutAll(userId);
        return ResponseEntity.ok().build();
    }

}