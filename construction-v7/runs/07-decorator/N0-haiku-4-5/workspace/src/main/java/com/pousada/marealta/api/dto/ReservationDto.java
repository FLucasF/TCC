package com.pousada.marealta.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDto(
    long id,
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    String ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    BigDecimal amountPaid,
    BigDecimal refund
) {}
