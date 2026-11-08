package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
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
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
