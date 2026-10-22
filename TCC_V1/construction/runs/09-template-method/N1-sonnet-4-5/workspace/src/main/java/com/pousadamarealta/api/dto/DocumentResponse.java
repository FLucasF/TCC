package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Reservation;

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
    public static DocumentResponse from(Reservation reservation) {
        return new DocumentResponse(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                reservation.getRoomType().name(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.calculateSubtotal(),
                reservation.calculateDiscount(),
                reservation.calculateTotal()
        );
    }
}
