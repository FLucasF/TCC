package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record DocumentDTO(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    RoomType roomType,
    String checkIn,
    String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static DocumentDTO from(Reservation reservation, Room room) {
        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            DATE_FORMAT.format(reservation.getCheckIn()),
            DATE_FORMAT.format(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
