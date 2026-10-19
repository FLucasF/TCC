package com.pousada.api.dto;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    Long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    RatePlan ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    ReservationStatus status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuest().getName(),
            reservation.getGuest().getEmail(),
            reservation.getGuest().getCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
