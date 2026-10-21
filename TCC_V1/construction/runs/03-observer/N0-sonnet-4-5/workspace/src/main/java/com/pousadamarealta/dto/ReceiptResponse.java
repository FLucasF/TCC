package com.pousadamarealta.dto;

import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptResponse(
    String title,
    long reservationId,
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
