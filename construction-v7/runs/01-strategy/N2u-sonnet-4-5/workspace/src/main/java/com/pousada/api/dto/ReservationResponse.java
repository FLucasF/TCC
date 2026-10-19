package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.RatePlanType;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

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
    @JsonProperty("ratePlan") RatePlanType ratePlan,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total,
    @JsonProperty("status") ReservationStatus status,
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
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlanType(),
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
