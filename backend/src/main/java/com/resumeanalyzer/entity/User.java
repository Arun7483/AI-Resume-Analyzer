package com.resumeanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_users_email",
                        columnList = "email",
                        unique = true
                ),
                @Index(
                        name = "idx_users_verification_token",
                        columnList = "verification_token"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 255
    )
    private String email;

    @Column(
            nullable = false,
            length = 255
    )
    private String password;

    @Column(
            name = "full_name",
            nullable = false,
            length = 100
    )
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(
            name = "email_verified",
            nullable = false
    )
    @Builder.Default
    private boolean emailVerified = false;

    @Column(
            name = "verification_token",
            length = 64
    )
    private String verificationToken;

    @Column(
            name = "verification_expires_at"
    )
    private Instant verificationExpiresAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Resume> resumes = new ArrayList<>();

    public enum Role {
        ROLE_USER,
        ROLE_ADMIN
    }
}