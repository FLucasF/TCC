package com.pousada.marealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.marealta.domain.Reservation;

import java.math.BigDecimal;

public record ReservationResponse(
    Long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
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
