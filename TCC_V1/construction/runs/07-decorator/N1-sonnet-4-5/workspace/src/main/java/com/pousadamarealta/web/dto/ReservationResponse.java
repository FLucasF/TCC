package com.pousadamarealta.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;

public record ReservationResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("roomNumber") String roomNumber,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestEmail") String guestEmail,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("checkIn") String checkIn,
        @JsonProperty("checkOut") String checkOut,
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
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
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
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                reservation.getStatus().name(),
                reservation.getAmountPaid(),
                reservation.getRefund()
        );
    }
}
