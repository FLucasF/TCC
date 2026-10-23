package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.model.Document;

import java.math.BigDecimal;

public record DocumentResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total,
    @JsonProperty("text") String text
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
            d.title(),
            d.reservationId(),
            d.guestName(),
            d.guestCpf(),
            d.companyCnpj(),
            d.companyName(),
            d.guestPassport(),
            d.guestCountry(),
            d.roomNumber(),
            d.roomType(),
            d.checkIn().toString(),
            d.checkOut().toString(),
            d.nights(),
            d.subtotal(),
            d.discount(),
            d.total(),
            d.text()
        );
    }
}
