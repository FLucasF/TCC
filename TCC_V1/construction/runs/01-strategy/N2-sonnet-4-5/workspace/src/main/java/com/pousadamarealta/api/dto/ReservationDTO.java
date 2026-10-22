package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDTO(
    long id,
    String roomNumber,
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
    public static ReservationDTO from(Reservation reservation) {
        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlanType().name(),
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
