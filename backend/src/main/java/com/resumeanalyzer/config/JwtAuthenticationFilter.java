package com.resumeanalyzer.config;

import com.resumeanalyzer.repository.UserRepository;
import com.resumeanalyzer.security.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.List;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {


    private final JwtService jwtService;

    private final UserRepository users;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    )
            throws ServletException, IOException {


        String header =
            request.getHeader(
                HttpHeaders.AUTHORIZATION
            );


        /*
         * Only process Bearer tokens.
         */
        if (
            header != null &&
            header.startsWith("Bearer ") &&
            SecurityContextHolder
                .getContext()
                .getAuthentication() == null
        ) {


            String token =
                header
                    .substring(7)
                    .trim();


            if (
                !token.isBlank() &&
                jwtService.isValid(token)
            ) {


                String email =
                    jwtService.subject(
                        token
                    );


                users.findByEmailIgnoreCase(
                    email
                )
                .ifPresent(user -> {


                    var authority =
                        new SimpleGrantedAuthority(
                            user
                                .getRole()
                                .name()
                        );


                    var authentication =
                        new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(authority)
                        );


                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                            authentication
                        );

                });
            }
        }


        chain.doFilter(
            request,
            response
        );
    }
}