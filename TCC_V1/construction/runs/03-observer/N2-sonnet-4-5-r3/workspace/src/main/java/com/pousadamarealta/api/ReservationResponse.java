package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousadamarealta.domain.Reservation;

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
    double subtotal,
    double discount,
    double total,
    String status,
    double amountPaid,
    Double refund
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.id(),
            reservation.roomNumber(),
            reservation.guestName(),
            reservation.guestEmail(),
            reservation.guestCpf(),
            reservation.checkIn(),
            reservation.checkOut(),
            reservation.guests(),
            reservation.ratePlan().name(),
            reservation.nights(),
            reservation.subtotal().doubleValue(),
            reservation.discount().doubleValue(),
            reservation.total().doubleValue(),
            reservation.status().name(),
            reservation.amountPaid().doubleValue(),
            reservation.refund() != null ? reservation.refund().doubleValue() : null
        );
    }
}
