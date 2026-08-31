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


    private final JwtAuthenticationFilter jwtFilter;


    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    )
            throws Exception {


        return http

            .csrf(csrf ->
                csrf.disable()
            )

            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource
                )
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(
                    (request, response, ex) ->
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Authentication required"
                        )
                )
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                )
                .permitAll()

                .requestMatchers(
                    "/api/v1/auth/**"
                )
                .permitAll()

                .requestMatchers(
                    "/error"
                )
                .permitAll()

                .requestMatchers(
                    "/actuator/health"
                )
                .permitAll()

                .anyRequest()
                .authenticated()
            )

            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            .build();

    }


    @Bean
    PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }


    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:*}")
            String allowedOrigin
    ) {

        CorsConfiguration configuration =
            new CorsConfiguration();


        /*
         * Your deployed Angular frontend.
         */

        configuration.setAllowedOriginPatterns(
            List.of(
                allowedOrigin,
                "https://*.onrender.com"
            )
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


        configuration.setAllowCredentials(
            true
        );


        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
            "/**",
            configuration
        );


        return source;

    }

}