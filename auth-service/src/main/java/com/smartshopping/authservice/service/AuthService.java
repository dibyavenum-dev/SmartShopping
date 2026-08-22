package com.smartshopping.authservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartshopping.authservice.entity.RefreshToken;
import com.smartshopping.authservice.entity.User;
import com.smartshopping.authservice.exception.UserAlreadyExistsException;
import com.smartshopping.authservice.exception.UserNotFoundException;
import com.smartshopping.authservice.repository.RefreshTokenRepository;
import com.smartshopping.authservice.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public User register(String username, String email, String password) {

        if (userRepository.findByUsername(username).isPresent()) {
        	throw new UserAlreadyExistsException(
        	        "Username already exists");
        }

        User user = new User();

        user.setUsername(username);

        user.setEmail(email);

        // Never store the raw password
        user.setPassword(passwordEncoder.encode(password));

        // Default role
        user.setRole("USER");

        return userRepository.save(user);
    }

    public User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                new UserNotFoundException("User not found"));
    }

    public boolean validatePassword(
            String rawPassword,
            String encodedPassword) {

        return passwordEncoder.matches(
                rawPassword,
                encodedPassword);
    }
    
    public void saveRefreshToken(
            String token,
            String username) {

        RefreshToken refreshToken =
                new RefreshToken(
                        token,
                        username,
                        false);

        refreshTokenRepository.save(refreshToken);
    }
    
    public boolean isRefreshTokenRevoked(String token) {

        return refreshTokenRepository
                .findByToken(token)
                .map(RefreshToken::isRevoked)
                .orElse(true);
    }
    
    public void revokeRefreshToken(String token) {

        refreshTokenRepository
                .findByToken(token)
                .ifPresent(refreshToken -> {

                    refreshToken.setRevoked(true);

                    refreshTokenRepository.save(refreshToken);
                });
    }
}