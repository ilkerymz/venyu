package com.venyu.identity.internal.repository;

import com.venyu.identity.internal.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // lower(email): users_email_uq expression index'iyle eşleşsin diye. Parametre normalize edilmiş olmalı.
    @Query("select u from User u where lower(u.email) = :email")
    Optional<User> findByEmail(String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = :email")
    boolean existsByEmail(String email);

    // Aynı kullanıcının eşzamanlı isteklerini sıraya sokar (SELECT ... FOR UPDATE).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(UUID id);

    boolean existsByPhoneAndPhoneVerifiedAtIsNotNullAndIdNot(String phone, UUID id);
}
