package com.resumeanalyzer.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
                // JWT APIs don't use CSRF tokens.
                .csrf(csrf -> csrf.disable())

                // Enable CORS.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // No HTTP session.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Return 401 instead of Spring's default login page.
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Authentication required"
                                        )
                        )
                )

                // API authorization rules.
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight.
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Authentication endpoints.
                        .requestMatchers(
                                "/api/v1/auth/**"
                        ).permitAll()

                        // Health check for Render.
                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        // Spring error endpoint.
                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // Everything else requires JWT.
                        .anyRequest().authenticated()
                )

                // JWT filter must run before Spring's username/password filter.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:*}") String allowedOrigin
    ) {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                List.of(allowedOrigin)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}