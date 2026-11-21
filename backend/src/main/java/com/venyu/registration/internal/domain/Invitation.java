package com.venyu.registration.internal.domain;

import com.venyu.registration.internal.exception.RegistrationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private String email;

    // Kabulden sonra da silinmez: tekrar tıklayan "geçersiz" yerine "zaten kullanıldı" görsün.
    @Column(nullable = false, updatable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(nullable = false, updatable = false)
    private UUID invitedBy;

    // Aynı link'e iki kişi aynı anda tıklarsa biri optimistic hata alır: davet tek kullanımlık.
    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant acceptedAt;

    public static Invitation create(UUID eventId, String email, String tokenHash, UUID invitedBy) {
        Invitation invitation = new Invitation();
        invitation.eventId = eventId;
        invitation.email = email;
        invitation.tokenHash = tokenHash;
        invitation.status = InvitationStatus.PENDING;
        invitation.invitedBy = invitedBy;
        invitation.createdAt = Instant.now();
        return invitation;
    }

    public void accept(Instant now) {
        if (status != InvitationStatus.PENDING) {
            throw new RegistrationStateException("Bu davet zaten kullanıldı.");
        }
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = now;
    }
}
