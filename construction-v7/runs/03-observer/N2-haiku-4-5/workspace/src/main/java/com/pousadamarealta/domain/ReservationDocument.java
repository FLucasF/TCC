package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDocument(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    int roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
}
