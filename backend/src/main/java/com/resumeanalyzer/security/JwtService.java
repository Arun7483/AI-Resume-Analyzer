package com.resumeanalyzer.security;

import com.resumeanalyzer.entity.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.Date;


@Service
public class JwtService {


    private final SecretKey key;

    private final long expirationMs;


    public JwtService(

            @Value("${app.jwt.secret}")
            String secret,

            @Value("${app.jwt.expiration:86400000}")
            long expirationMs

    ) {

        this.key =
            resolveKey(secret);

        this.expirationMs =
            expirationMs;
    }


    public String generate(
            User user
    ) {

        Instant now =
            Instant.now();


        return Jwts.builder()

            .subject(
                user.getEmail()
            )

            .claim(
                "role",
                user.getRole().name()
            )

            .issuedAt(
                Date.from(now)
            )

            .expiration(
                Date.from(
                    now.plusMillis(
                        expirationMs
                    )
                )
            )

            .signWith(key)

            .compact();
    }


    public String subject(
            String token
    ) {

        return Jwts.parser()

            .verifyWith(key)

            .build()

            .parseSignedClaims(token)

            .getPayload()

            .getSubject();
    }


    public boolean isValid(
            String token
    ) {

        try {

            subject(token);

            return true;

        } catch (
            JwtException |
            IllegalArgumentException ex
        ) {

            return false;
        }
    }


    public long expirationSeconds() {

        return expirationMs / 1000;
    }


    private static SecretKey resolveKey(
            String secret
    ) {


        if (
            secret == null ||
            secret.isBlank()
        ) {

            throw new IllegalArgumentException(
                "JWT secret must not be blank. " +
                "Configure JWT_SECRET."
            );
        }


        String trimmed =
            secret.trim();


        /*
         * Try Base64 first.
         */
        try {

            byte[] decoded =
                Decoders.BASE64.decode(
                    trimmed
                );


            if (
                decoded.length >= 32
            ) {

                return Keys.hmacShaKeyFor(
                    decoded
                );
            }

        } catch (
            IllegalArgumentException ignored
        ) {

            /*
             * Not Base64.
             * Continue as normal text.
             */
        }


        /*
         * Normal text secret.
         *
         * HS256 requires at least 32 bytes.
         */
        byte[] raw =
            trimmed.getBytes(
                StandardCharsets.UTF_8
            );


        if (
            raw.length < 32
        ) {

            throw new IllegalArgumentException(
                "JWT secret must be at least 32 characters long."
            );
        }


        return Keys.hmacShaKeyFor(
            raw
        );
    }
}