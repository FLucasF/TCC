package com.pousadamarealta.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
    int id,
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    RatePlan ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    ReservationStatus status,
    BigDecimal amountPaid,
    BigDecimal refund
) {}
