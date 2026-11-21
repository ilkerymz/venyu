package com.venyu.event.internal.web.dto;

import com.venyu.event.internal.domain.TicketDetails;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// capacity null = sınırsız (sadece adresli etkinlikte).
public record TicketTypeRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @Min(1) @Max(100_000) Integer capacity) {

    public TicketDetails toDetails() {
        return new TicketDetails(name, description, price, capacity);
    }
}
