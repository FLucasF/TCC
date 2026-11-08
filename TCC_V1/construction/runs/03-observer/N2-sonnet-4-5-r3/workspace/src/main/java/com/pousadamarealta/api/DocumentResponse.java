package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    int roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    double subtotal,
    double discount,
    double total
) {
    public static DocumentResponse from(Reservation reservation, RoomType roomType) {
        return new DocumentResponse(
            "Comprovante de reserva",
            reservation.id(),
            reservation.guestName(),
            reservation.guestCpf(),
            reservation.roomNumber(),
            roomType.name(),
            reservation.checkIn(),
            reservation.checkOut(),
            reservation.nights(),
            reservation.subtotal().doubleValue(),
            reservation.discount().doubleValue(),
            reservation.total().doubleValue()
        );
    }
}
