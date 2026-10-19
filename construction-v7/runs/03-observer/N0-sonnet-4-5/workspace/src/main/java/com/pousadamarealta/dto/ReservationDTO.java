package com.pousadamarealta.dto;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record ReservationDTO(
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
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    ReservationStatus status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static ReservationDTO from(Reservation reservation) {
        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            DATE_FORMAT.format(reservation.getCheckIn()),
            DATE_FORMAT.format(reservation.getCheckOut()),
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
