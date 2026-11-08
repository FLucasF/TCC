package com.marealta.pousada.dto;

import com.marealta.pousada.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDto(
    long id,
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    String ratePlan,
    long nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationDto from(Reservation reservation) {
        return new ReservationDto(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlanName(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
