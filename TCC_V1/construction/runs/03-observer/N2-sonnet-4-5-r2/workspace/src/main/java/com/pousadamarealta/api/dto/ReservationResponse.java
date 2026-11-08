package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;

public record ReservationResponse(
    long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    RatePlan ratePlan,
    int nights,
    double subtotal,
    double discount,
    double total,
    ReservationStatus status,
    double amountPaid,
    Double refund
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getGuests(),
            reservation.getRatePlan(),
            reservation.getNights(),
            reservation.getSubtotal().doubleValue(),
            reservation.getDiscount().doubleValue(),
            reservation.getTotal().doubleValue(),
            reservation.getStatus(),
            reservation.getAmountPaid().doubleValue(),
            reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null
        );
    }
}
