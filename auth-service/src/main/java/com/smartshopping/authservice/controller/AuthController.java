package com.smartshopping.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import com.smartshopping.authservice.dto.AuthResponse;
import com.smartshopping.authservice.dto.LoginRequest;
import com.smartshopping.authservice.dto.RegisterRequest;
import com.smartshopping.authservice.entity.User;
import com.smartshopping.authservice.security.JwtService;
import com.smartshopping.authservice.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService) {

        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

    	User user = authService.register(
    	        request.getUsername(),
    	        request.getEmail(),
    	        request.getPassword());

        return ResponseEntity.ok(
                "User registered successfully: "
                        + user.getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        User user =
                authService.findByUsername(
                        request.getUsername());

        boolean valid =
                authService.validatePassword(
                        request.getPassword(),
                        user.getPassword());

        if (!valid) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid username or password");
        }

        String accessToken =
                jwtService.generateToken(
                        user.getUsername());

        String refreshToken =
                jwtService.generateRefreshToken(
                        user.getUsername());

        AuthResponse response =
                new AuthResponse(
                        accessToken,
                        refreshToken);

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            Authentication authentication) {

        String username = authentication.getName();

        User user = authService.findByUsername(username);

        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestParam String refreshToken) {

        try {

            if (!jwtService.isTokenValid(refreshToken)) {
                return ResponseEntity
                        .status(401)
                        .body("Invalid or expired refresh token");
            }

            String username =
                    jwtService.extractUsername(refreshToken);

            String newAccessToken =
                    jwtService.generateToken(username);

            return ResponseEntity.ok(newAccessToken);

        } catch (Exception e) {

            return ResponseEntity
                    .status(401)
                    .body("Invalid or expired refresh token");
        }
    }
}