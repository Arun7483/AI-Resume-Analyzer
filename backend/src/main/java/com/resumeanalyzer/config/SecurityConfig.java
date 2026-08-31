package com.resumeanalyzer.config;

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

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;


    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;


    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        return http

            /*
             * REST API does not use CSRF tokens.
             */
            .csrf(csrf ->
                csrf.disable()
            )


            /*
             * Enable CORS.
             */
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource
                )
            )


            /*
             * JWT authentication is stateless.
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )


            /*
             * Authorization rules.
             */
            .authorizeHttpRequests(auth -> auth

                /*
                 * Browser CORS preflight.
                 */
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                )
                .permitAll()


                /*
                 * Login/register/verification.
                 */
                .requestMatchers(
                    "/api/v1/auth/**"
                )
                .permitAll()


                /*
                 * Health check.
                 */
                .requestMatchers(
                    "/actuator/health"
                )
                .permitAll()


                /*
                 * Error endpoint.
                 */
                .requestMatchers(
                    "/error"
                )
                .permitAll()


                /*
                 * Everything else needs JWT.
                 */
                .anyRequest()
                .authenticated()
            )


            /*
             * JWT filter runs before Spring's
             * username/password filter.
             */
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
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
            new CorsConfiguration();


        /*
         * Allow the Render frontend.
         *
         * Multiple origins can be comma-separated.
         */
        configuration.setAllowedOrigins(
            Arrays.stream(
                allowedOrigin.split(",")
            )
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList()
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


        configuration.setExposedHeaders(
            List.of("Authorization")
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