package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.models.RatePlan;
import com.pousada.models.Reservation;
import com.pousada.models.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
        @JsonProperty("id")
        long id,
        @JsonProperty("roomNumber")
        int roomNumber,
        @JsonProperty("guestName")
        String guestName,
        @JsonProperty("guestEmail")
        String guestEmail,
        @JsonProperty("guestCpf")
        String guestCpf,
        @JsonProperty("checkIn")
        LocalDate checkIn,
        @JsonProperty("checkOut")
        LocalDate checkOut,
        @JsonProperty("guests")
        int guests,
        @JsonProperty("ratePlan")
        RatePlan ratePlan,
        @JsonProperty("nights")
        long nights,
        @JsonProperty("subtotal")
        BigDecimal subtotal,
        @JsonProperty("discount")
        BigDecimal discount,
        @JsonProperty("total")
        BigDecimal total,
        @JsonProperty("status")
        ReservationStatus status,
        @JsonProperty("amountPaid")
        BigDecimal amountPaid,
        @JsonProperty("refund")
        BigDecimal refund
) {
    public static ReservationResponse fromReservation(Reservation res) {
        return new ReservationResponse(
                res.getId(),
                res.getRoomNumber(),
                res.getGuestName(),
                res.getGuestEmail(),
                res.getGuestCpf(),
                res.getCheckIn(),
                res.getCheckOut(),
                res.getGuests(),
                res.getRatePlan(),
                res.getNights(),
                res.getSubtotal(),
                res.getDiscount(),
                res.getTotal(),
                res.getStatus(),
                res.getAmountPaid(),
                res.getRefund()
        );
    }
}
