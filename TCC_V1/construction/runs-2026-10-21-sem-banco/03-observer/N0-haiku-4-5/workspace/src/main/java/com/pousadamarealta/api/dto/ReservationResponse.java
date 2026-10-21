package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;
import java.time.LocalDate;

public record ReservationResponse(
    @JsonProperty("id") long id,
    @JsonProperty("roomNumber") int roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("guests") int guests,
    @JsonProperty("ratePlan") String ratePlan,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") double subtotal,
    @JsonProperty("discount") double discount,
    @JsonProperty("total") double total,
    @JsonProperty("status") String status,
    @JsonProperty("amountPaid") double amountPaid,
    @JsonProperty("refund") Double refund
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
            r.id,
            r.roomNumber,
            r.guestName,
            r.guestEmail,
            r.guestCpf,
            formatDate(r.checkIn),
            formatDate(r.checkOut),
            r.guests,
            r.ratePlan.name(),
            r.nights,
            r.subtotal,
            r.discount,
            r.total,
            r.status.name(),
            r.amountPaid,
            r.refund
        );
    }

    private static String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
