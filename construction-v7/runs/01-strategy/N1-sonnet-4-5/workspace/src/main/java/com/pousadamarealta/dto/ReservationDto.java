package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.RatePlanType;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDto(
        @JsonProperty("id") long id,
        @JsonProperty("roomNumber") int roomNumber,
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
    public static ReservationDto from(Reservation reservation) {
        return new ReservationDto(
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getGuestName(),
                reservation.getGuestEmail(),
                reservation.getGuestCpf(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
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
