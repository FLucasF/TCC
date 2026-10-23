package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationDTO(
    long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
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
    public static ReservationDTO from(Reservation reservation, Room room) {
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(room.getNightlyRate());

        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            subtotal.doubleValue(),
            discount.doubleValue(),
            total.doubleValue(),
            reservation.getStatus().name(),
            reservation.getAmountPaid().doubleValue(),
            reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null
        );
    }
}
