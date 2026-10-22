package com.pousadamarealta.domain.model;

import com.pousadamarealta.domain.enums.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Document(
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
