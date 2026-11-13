package com.venyu.identity.internal;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private Instant emailVerifiedAt;

    @Column(nullable = false)
    private String displayName;

    private String phone;

    private Instant phoneVerifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Getter(AccessLevel.NONE)
    private Set<Role> roles = new HashSet<>();

    public static User register(String email, String passwordHash, String displayName) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.createdAt = Instant.now();
        user.roles.add(Role.PARTICIPANT);
        return user;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }
}
