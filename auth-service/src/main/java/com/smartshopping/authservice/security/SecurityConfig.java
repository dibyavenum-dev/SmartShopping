package com.smartshopping.authservice.security;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(
	        JwtAuthenticationFilter jwtAuthenticationFilter) {

	    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

            	    .requestMatchers(
            	        "/auth/register",
            	        "/auth/login",
            	        "/auth/refresh",
            	        "/auth/logout"
            	    ).permitAll()

            	    .requestMatchers(
            	        HttpMethod.GET,
            	        "/products/**"
            	    ).hasAnyRole("USER", "ADMIN")

            	    .requestMatchers(
            	        HttpMethod.POST,
            	        "/products/**"
            	    ).hasRole("ADMIN")

            	    .requestMatchers(
            	        HttpMethod.PUT,
            	        "/products/**"
            	    ).hasRole("ADMIN")

            	    .requestMatchers(
            	        HttpMethod.DELETE,
            	        "/products/**"
            	    ).hasRole("ADMIN")

            	    .requestMatchers("/orders/**")
            	    .hasAnyRole("USER", "ADMIN")

            	    .anyRequest().authenticated()
            	)

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}