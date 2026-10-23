package com.pousada.marealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;

import java.math.BigDecimal;

public record DocumentResponse(
    String title,
    @JsonProperty("reservationId") Long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentResponse from(Reservation reservation, Room room) {
        return new DocumentResponse(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
