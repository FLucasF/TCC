package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentDto(
        @JsonProperty("title") String title,
        @JsonProperty("reservationId") long reservationId,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("roomNumber") int roomNumber,
        @JsonProperty("roomType") RoomType roomType,
        @JsonProperty("checkIn") LocalDate checkIn,
        @JsonProperty("checkOut") LocalDate checkOut,
        @JsonProperty("nights") int nights,
        @JsonProperty("subtotal") BigDecimal subtotal,
        @JsonProperty("discount") BigDecimal discount,
        @JsonProperty("total") BigDecimal total
) {
    public static DocumentDto from(Reservation reservation, Room room) {
        return new DocumentDto(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }
}
