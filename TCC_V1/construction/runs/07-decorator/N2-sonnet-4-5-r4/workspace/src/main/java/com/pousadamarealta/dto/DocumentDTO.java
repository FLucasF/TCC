package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentDTO(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    double subtotal,
    Double discount,
    double total
) {
    public static DocumentDTO from(Reservation reservation, Room room) {
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(room.getNightlyRate());

        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getNights(),
            subtotal.doubleValue(),
            discount.compareTo(BigDecimal.ZERO) > 0 ? discount.doubleValue() : null,
            total.doubleValue()
        );
    }
}
