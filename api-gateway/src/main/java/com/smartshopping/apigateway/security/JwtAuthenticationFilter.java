package com.smartshopping.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        HttpMethod method =
                exchange.getRequest()
                        .getMethod();

        // Auth APIs should be publicly accessible
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        String authorizationHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        // No Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            return unauthorized(exchange);
        }

        String token =
                authorizationHeader.substring(7);

        // Invalid or expired JWT
        if (!jwtService.isTokenValid(token)) {

            return unauthorized(exchange);
        }

        String role;

        try {
            role = jwtService.extractRole(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }

        /*
         * Product authorization
         *
         * GET    -> USER + ADMIN
         * POST   -> ADMIN
         * PUT    -> ADMIN
         * DELETE -> ADMIN
         */
        if (path.startsWith("/products")) {

            if (HttpMethod.GET.equals(method)) {

                if (!hasAnyRole(role, "USER", "ADMIN")) {
                    return forbidden(exchange);
                }

            } else if (HttpMethod.POST.equals(method)
                    || HttpMethod.PUT.equals(method)
                    || HttpMethod.DELETE.equals(method)) {

                if (!"ADMIN".equals(role)) {
                    return forbidden(exchange);
                }
            }
        }

        /*
         * Order APIs
         *
         * USER + ADMIN
         */
        if (path.startsWith("/orders")) {

            if (!hasAnyRole(role, "USER", "ADMIN")) {
                return forbidden(exchange);
            }
        }

        // JWT + role are valid → forward request
        return chain.filter(exchange);
    }

    private boolean hasAnyRole(
            String role,
            String... allowedRoles) {

        for (String allowedRole : allowedRoles) {

            if (allowedRole.equals(role)) {
                return true;
            }
        }

        return false;
    }

    private Mono<Void> unauthorized(
            ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(
                        HttpStatus.UNAUTHORIZED);

        return exchange.getResponse()
                .setComplete();
    }

    private Mono<Void> forbidden(
            ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(
                        HttpStatus.FORBIDDEN);

        return exchange.getResponse()
                .setComplete();
    }
}