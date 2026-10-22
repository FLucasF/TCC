package com.pousadamarealta.controller.dto;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;

public record ReservationDto(
    Long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
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
    public static ReservationDto from(Reservation reservation) {
        return new ReservationDto(
            reservation.id(),
            reservation.roomNumber(),
            reservation.guestName(),
            reservation.guestEmail(),
            reservation.guestCpf(),
            reservation.checkIn().toString(),
            reservation.checkOut().toString(),
            reservation.guests(),
            reservation.ratePlan().name(),
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total(),
            reservation.status().name(),
            reservation.amountPaid(),
            reservation.refund()
        );
    }
}
