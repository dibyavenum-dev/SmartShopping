package com.smartshopping.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import org.slf4j.MDC;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {

        RestTemplate restTemplate =
                new RestTemplate();

        restTemplate.getInterceptors().add(
                correlationIdInterceptor());

        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor correlationIdInterceptor() {

        return (request, body, execution) -> {

            String correlationId =
                    MDC.get("X-Correlation-Id");

            if (correlationId != null &&
                    !correlationId.isBlank()) {

                request.getHeaders().set(
                        "X-Correlation-Id",
                        correlationId);
            }

            return execution.execute(
                    request,
                    body);
        };
    }
}