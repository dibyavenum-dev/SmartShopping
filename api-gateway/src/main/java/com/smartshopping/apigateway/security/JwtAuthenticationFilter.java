package com.smartshopping.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            ObjectMapper objectMapper) {

        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
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

        // Auth APIs are publicly accessible
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

            return errorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required");
        }

        String token =
                authorizationHeader.substring(7);

        // Invalid or expired JWT
        if (!jwtService.isTokenValid(token)) {

            return errorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token");
        }

        String role;

        try {

            role = jwtService.extractRole(token);

        } catch (Exception e) {

            return errorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token");
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
                    return errorResponse(
                            exchange,
                            HttpStatus.FORBIDDEN,
                            "Access denied");
                }

            } else if (HttpMethod.POST.equals(method)
                    || HttpMethod.PUT.equals(method)
                    || HttpMethod.DELETE.equals(method)) {

                if (!"ADMIN".equals(role)) {
                    return errorResponse(
                            exchange,
                            HttpStatus.FORBIDDEN,
                            "Access denied");
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

                return errorResponse(
                        exchange,
                        HttpStatus.FORBIDDEN,
                        "Access denied");
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

    private Mono<Void> errorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            String message) {

        GatewayErrorResponse error =
                new GatewayErrorResponse(
                        status.value(),
                        message);

        String json;

        try {

            json = objectMapper.writeValueAsString(error);

        } catch (JsonProcessingException e) {

            json = "{\"status\":"
                    + status.value()
                    + ",\"message\":\""
                    + message
                    + "\"}";
        }

        exchange.getResponse()
                .setStatusCode(status);

        exchange.getResponse()
                .getHeaders()
                .setContentType(
                        MediaType.APPLICATION_JSON);

        byte[] bytes =
                json.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8);

        return exchange.getResponse()
                .writeWith(
                        Mono.just(
                                exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(bytes)));
    }
}