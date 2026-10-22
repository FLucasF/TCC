package com.pousadamarealta.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

public record DocumentResponse(
        @JsonProperty("title") String title,
        @JsonProperty("reservationId") Long reservationId,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("roomNumber") String roomNumber,
        @JsonProperty("roomType") String roomType,
        @JsonProperty("checkIn") String checkIn,
        @JsonProperty("checkOut") String checkOut,
        @JsonProperty("nights") int nights,
        @JsonProperty("subtotal") BigDecimal subtotal,
        @JsonProperty("discount") BigDecimal discount,
        @JsonProperty("total") BigDecimal total
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
