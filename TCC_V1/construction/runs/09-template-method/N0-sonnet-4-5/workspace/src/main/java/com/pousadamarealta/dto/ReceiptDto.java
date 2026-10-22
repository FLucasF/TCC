package com.pousadamarealta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptDto(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
}
