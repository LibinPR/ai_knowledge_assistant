package com.yourorg.aiknowledgeassistant.auth;

import java.util.Set;

public sealed interface AuthenticationResult permits AuthenticationResult.Success , AuthenticationResult.Failure {

    record Success(Long userId , String email , Set<String> roles , int tokenVersion) implements AuthenticationResult {}

    record Failure(String reason) implements AuthenticationResult {}
}


