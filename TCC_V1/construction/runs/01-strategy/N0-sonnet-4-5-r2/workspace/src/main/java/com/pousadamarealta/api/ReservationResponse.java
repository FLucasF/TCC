package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    @JsonProperty("id") Long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    @JsonProperty("guests") int guests,
    @JsonProperty("ratePlan") String ratePlan,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total,
    @JsonProperty("status") String status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
    @JsonProperty("refund") BigDecimal refund
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
