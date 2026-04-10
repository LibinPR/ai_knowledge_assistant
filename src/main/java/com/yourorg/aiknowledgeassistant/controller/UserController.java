package com.yourorg.aiknowledgeassistant.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public String userOnly(){
        return "User profile";
    }

    @GetMapping("/whoami")
    public Object whoami(Authentication authentication){

        return Map.of(
                "principal",authentication.getPrincipal(),
                "authorities",authentication.getAuthorities()
        );
    }
}
