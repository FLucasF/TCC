package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.repository.RoomRepository;

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
    public static DocumentResponse from(Reservation r) {
        return new DocumentResponse(
            "Comprovante de reserva",
            r.getId(),
            r.getGuestName(),
            r.getGuestCpf(),
            r.getRoomNumber(),
            RoomRepository.getRoomType(r.getRoomNumber()).name(),
            r.getCheckIn(),
            r.getCheckOut(),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal()
        );
    }
}
