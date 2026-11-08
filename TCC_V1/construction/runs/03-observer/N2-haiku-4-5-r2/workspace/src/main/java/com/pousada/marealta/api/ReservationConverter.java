package com.pousada.marealta.api;

import com.pousada.marealta.domain.Reservation;

import java.time.format.DateTimeFormatter;

public class ReservationConverter {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static ReservationResponse toResponse(Reservation reservation) {
    Double refund = reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null;

    return new ReservationResponse(
        reservation.getId(),
        reservation.getRoomNumber(),
        reservation.getGuestName(),
        reservation.getGuestEmail(),
        reservation.getGuestCpf(),
        reservation.getCheckIn().format(DATE_FORMATTER),
        reservation.getCheckOut().format(DATE_FORMATTER),
        reservation.getGuests(),
        reservation.getRatePlan().name(),
        reservation.getNights(),
        reservation.getSubtotal().doubleValue(),
        reservation.getDiscount().doubleValue(),
        reservation.getTotal().doubleValue(),
        reservation.getStatus().name(),
        reservation.getAmountPaid().doubleValue(),
        refund
    );
  }
}
