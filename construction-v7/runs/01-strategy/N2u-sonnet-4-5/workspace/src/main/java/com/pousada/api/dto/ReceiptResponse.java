package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Reservation;
import com.pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") Long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") RoomType roomType,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total
) {
    public static ReceiptResponse from(Reservation reservation, RoomType roomType) {
        return new ReceiptResponse(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            roomType,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
