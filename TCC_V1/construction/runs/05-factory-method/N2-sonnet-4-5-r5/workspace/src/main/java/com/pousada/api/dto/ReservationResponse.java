package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.model.Reservation;

import java.math.BigDecimal;

public record ReservationResponse(
    @JsonProperty("id") long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
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
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
            r.id(),
            r.roomNumber(),
            r.guestName(),
            r.guestEmail(),
            r.guestType().guestCpf(),
            r.guestType().companyCnpj(),
            r.guestType().companyName(),
            r.guestType().guestPassport(),
            r.guestType().guestCountry(),
            r.checkIn().toString(),
            r.checkOut().toString(),
            r.guests(),
            r.ratePlan().name(),
            r.nights(),
            r.subtotal(),
            r.discount(),
            r.total(),
            r.status().name(),
            r.amountPaid(),
            r.refund()
        );
    }
}
