package com.pousadamarealta.api;

import com.pousadamarealta.domain.ReservationDocument;
import com.pousadamarealta.domain.RoomType;

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
    public static DocumentResponse from(ReservationDocument document) {
        return new DocumentResponse(
            document.title(),
            document.reservationId(),
            document.guestName(),
            document.guestCpf(),
            document.roomNumber(),
            document.roomType(),
            document.checkIn(),
            document.checkOut(),
            document.nights(),
            document.subtotal(),
            document.discount(),
            document.total()
        );
    }
}
