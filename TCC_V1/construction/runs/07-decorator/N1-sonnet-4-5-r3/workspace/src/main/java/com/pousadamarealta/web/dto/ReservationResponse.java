package com.pousadamarealta.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    int guests,
    @JsonProperty("ratePlan") String ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
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
            r.calculateSubtotal(),
            r.calculateDiscount(),
            r.calculateTotal(),
            r.getStatus().name(),
            r.getAmountPaid(),
            r.getRefund()
        );
    }
}
