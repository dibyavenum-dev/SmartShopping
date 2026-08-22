package com.smartshopping.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshopping.authservice.entity.RefreshToken;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);
}