package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousadamarealta.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
    Long id,
    String roomNumber,
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
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
            r.getId(),
            r.getRoomNumber(),
            r.getGuestName(),
            r.getGuestEmail(),
            r.getGuestCpf(),
            r.getCheckIn(),
            r.getCheckOut(),
            r.getGuests(),
            r.getRatePlan().name(),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal(),
            r.getStatus().name(),
            r.getAmountPaid(),
            r.getRefund()
        );
    }
}
