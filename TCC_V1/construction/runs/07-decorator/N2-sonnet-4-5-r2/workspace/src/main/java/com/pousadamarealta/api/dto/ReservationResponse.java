package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
    long id,
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    String ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
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
