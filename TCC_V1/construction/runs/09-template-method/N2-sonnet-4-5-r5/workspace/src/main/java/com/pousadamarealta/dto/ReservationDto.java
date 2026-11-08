package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDto(
    Long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkIn,
    @JsonFormat(pattern = "yyyy-MM-dd")
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
    public static ReservationDto from(Reservation r) {
        return new ReservationDto(
            r.getId(),
            r.getRoomNumber(),
            r.getGuestName(),
            r.getGuestEmail(),
            r.getGuestCpf(),
            r.getCheckIn(),
            r.getCheckOut(),
            r.getGuests(),
            r.getRatePlan(),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal(),
            r.getStatus(),
            r.getAmountPaid(),
            r.getRefund()
        );
    }
}
