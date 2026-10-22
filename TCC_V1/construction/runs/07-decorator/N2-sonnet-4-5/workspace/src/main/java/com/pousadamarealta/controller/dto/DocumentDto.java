package com.pousadamarealta.controller.dto;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

public record DocumentDto(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentDto from(Reservation reservation, Room room) {
        return new DocumentDto(
            "Comprovante de reserva",
            reservation.id(),
            reservation.guestName(),
            reservation.guestCpf(),
            room.number(),
            room.type().name(),
            reservation.checkIn().toString(),
            reservation.checkOut().toString(),
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total()
        );
    }
}
