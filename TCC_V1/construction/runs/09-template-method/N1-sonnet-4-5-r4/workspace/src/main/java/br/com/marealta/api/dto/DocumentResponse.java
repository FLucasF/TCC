package br.com.marealta.api.dto;

import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    RoomType roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
}
