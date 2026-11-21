package com.venyu.event.internal.domain;

import com.venyu.event.internal.exception.InvalidTicketException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketType {

    public static final String DEFAULT_CURRENCY = "TRY";
    public static final String DEFAULT_NAME = "Genel Katılım";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, updatable = false)
    private String currency;

    private Integer capacity;

    // ⚠️ Sadece atomik sorgularla değişir (reserveSeat / releaseSeat). updatable = false:
    // Hibernate entity'yi güncellerken bellekteki eski değeri yazıp eşzamanlı satışları ezmesin.
    @Column(nullable = false, updatable = false)
    private int sold;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static TicketType define(UUID eventId, TicketDetails details) {
        TicketType ticket = new TicketType();
        ticket.eventId = eventId;
        ticket.currency = DEFAULT_CURRENCY;
        ticket.createdAt = Instant.now();
        ticket.apply(details, ticket.createdAt);
        return ticket;
    }

    public void update(TicketDetails details, Instant now) {
        BigDecimal newPrice = normalize(details.price());
        if (hasSales() && newPrice.compareTo(price) != 0) {
            throw new InvalidTicketException("Satışı başlamış biletin fiyatı değiştirilemez.");
        }
        if (details.capacity() != null && details.capacity() < sold) {
            throw new InvalidTicketException("Kapasite satılan bilet sayısının (" + sold + ") altına inemez.");
        }
        apply(details, now);
    }

    public boolean hasSales() {
        return sold > 0;
    }

    public boolean isFree() {
        return price.signum() == 0;
    }

    // Null = sınırsız.
    public Integer remaining() {
        return capacity == null ? null : capacity - sold;
    }

    private void apply(TicketDetails details, Instant now) {
        this.name = details.name();
        this.description = details.description();
        this.price = normalize(details.price());
        this.capacity = details.capacity();
        this.updatedAt = now;
    }

    // "150", "150.0", "150.00" → hepsi 150.00. Fazla basamak DTO'da @Digits ile zaten reddediliyor.
    private static BigDecimal normalize(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_EVEN);
    }
}
