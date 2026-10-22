package com.pousadamarealta.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(
    String title,
    int reservationId,
    String guestName,
    String guestCpf,
    int roomNumber,
    RoomType roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {}
