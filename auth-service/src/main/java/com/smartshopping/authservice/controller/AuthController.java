package com.smartshopping.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
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

        String token =
                jwtService.generateToken(
                        user.getUsername());

        return ResponseEntity.ok(token);
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            Authentication authentication) {

        String username = authentication.getName();

        User user = authService.findByUsername(username);

        return ResponseEntity.ok(user);
    }
}