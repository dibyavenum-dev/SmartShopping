package com.smartshopping.orderservice.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId =
                request.getHeader(CORRELATION_ID);

        if (correlationId == null ||
                correlationId.isBlank()) {

            correlationId = "UNKNOWN";
        }

        try {

            MDC.put(
                    CORRELATION_ID,
                    correlationId);

            log.info(
                    "Correlation ID received: {}",
                    correlationId);

            filterChain.doFilter(
                    request,
                    response);

        } finally {

            MDC.remove(CORRELATION_ID);
        }
    }
}