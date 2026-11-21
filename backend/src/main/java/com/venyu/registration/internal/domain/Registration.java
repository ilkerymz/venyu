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
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID ticketTypeId;

    // Kimlik: ya userId (üye) ya guestEmail (misafir). DB'de registrations_one_identity_chk.
    @Column(updatable = false)
    private UUID userId;

    @Column(updatable = false)
    private String guestEmail;

    @Column(updatable = false)
    private String guestName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    // Sadece onay bekleyen misafir kaydında dolu. Ham token saklanmaz (ADR-004).
    private String confirmationTokenHash;

    private Instant expiresAt;

    // Görünen form alanlarının cevapları (ADR-009). JSONB kolonu Java'da String (form_schemas ile aynı yaklaşım).
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String answers;

    @Column(updatable = false)
    private Integer formVersion;

    // QR'daki bilet kodu: onaylanınca verilir, sonra değişmez. Giriş görevlisine gösterildiği için yetki vermez.
    private String ticketCode;

    // Sadece misafir: bilet e-postasındaki "yönet" link'i (görüntüle, iptal). Bilet kodundan ayrı (ADR-010).
    private String manageTokenHash;

    // Check-in atomik koşullu UPDATE ile yapılır (RegistrationRepository.checkIn); entity bu alanları yazmaz.
    @Column(insertable = false, updatable = false)
    private Instant checkedInAt;

    @Column(insertable = false, updatable = false)
    private UUID checkedInBy;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant cancelledAt;

    // Üye, ücretsiz bilet: kayıt anında onaylı. Ücretli akış (RESERVED → PENDING_THRESHOLD) F3'te.
    public static Registration confirmed(UUID eventId, UUID ticketTypeId, UUID userId, FormAnswers answers,
                                         TicketCredentials ticket) {
        Registration registration = create(eventId, ticketTypeId, RegistrationStatus.CONFIRMED, answers);
        registration.userId = userId;
        registration.issue(ticket);
        return registration;
    }

    // Misafir: e-posta onaylanana kadar koltuk expiresAt'e kadar tutulur.
    public static Registration reservedGuest(UUID eventId, UUID ticketTypeId, String email, String name,
                                             FormAnswers answers, String tokenHash, Instant expiresAt) {
        Registration registration = create(eventId, ticketTypeId, RegistrationStatus.RESERVED, answers);
        registration.guestEmail = email;
        registration.guestName = name;
        registration.confirmationTokenHash = tokenHash;
        registration.expiresAt = expiresAt;
        return registration;
    }

    // Davetli misafir: e-postası davet link'iyle kanıtlandı, onay adımı yok (F2-4 K1).
    public static Registration confirmedGuest(UUID eventId, UUID ticketTypeId, String email, String name,
                                              FormAnswers answers, TicketCredentials ticket) {
        Registration registration = create(eventId, ticketTypeId, RegistrationStatus.CONFIRMED, answers);
        registration.guestEmail = email;
        registration.guestName = name;
        registration.issue(ticket);
        return registration;
    }

    private static Registration create(UUID eventId, UUID ticketTypeId, RegistrationStatus status,
                                       FormAnswers answers) {
        Registration registration = new Registration();
        registration.eventId = eventId;
        registration.ticketTypeId = ticketTypeId;
        registration.status = status;
        registration.answers = answers.json();
        registration.formVersion = answers.formVersion();
        registration.createdAt = Instant.now();
        registration.updatedAt = registration.createdAt;
        return registration;
    }

    public void confirm(Instant now, TicketCredentials ticket) {
        if (status == RegistrationStatus.EXPIRED || (status == RegistrationStatus.RESERVED && !expiresAt.isAfter(now))) {
            throw new RegistrationStateException("Onay süresi doldu. Lütfen tekrar kayıt ol.");
        }
        if (status != RegistrationStatus.RESERVED) {
            throw new RegistrationStateException("Bu kayıt onay beklemiyor.");
        }
        this.status = RegistrationStatus.CONFIRMED;
        this.confirmationTokenHash = null; // tek kullanımlık
        this.expiresAt = null;
        this.updatedAt = now;
        issue(ticket);
    }

    // Token hash'i bilerek silinmez: geç tıklayan misafir "geçersiz link" yerine "süre doldu" görsün.
    public void expire(Instant now) {
        if (status != RegistrationStatus.RESERVED) {
            throw new RegistrationStateException("Sadece onay bekleyen kayıtların süresi dolabilir.");
        }
        this.status = RegistrationStatus.EXPIRED;
        this.updatedAt = now;
    }

    private void issue(TicketCredentials ticket) {
        this.ticketCode = ticket.ticketCode();
        this.manageTokenHash = ticket.manageTokenHash();
    }

    public void cancel(Instant now) {
        if (status != RegistrationStatus.CONFIRMED) {
            throw new RegistrationStateException("Sadece onaylı kayıtlar iptal edilebilir.");
        }
        this.status = RegistrationStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }
}
