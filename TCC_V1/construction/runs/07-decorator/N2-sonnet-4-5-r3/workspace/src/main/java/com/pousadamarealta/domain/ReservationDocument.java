package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDocument(
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
) {}
