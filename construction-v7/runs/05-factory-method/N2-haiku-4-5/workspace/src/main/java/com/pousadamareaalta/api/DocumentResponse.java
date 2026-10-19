package com.pousadamareaalta.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamareaalta.model.Document;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("roomNumber") int roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total,
    @JsonProperty("text") String text
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
            doc.title(),
            doc.reservationId(),
            doc.guestName(),
            doc.guestCpf(),
            doc.companyCnpj(),
            doc.companyName(),
            doc.guestPassport(),
            doc.guestCountry(),
            doc.roomNumber(),
            doc.roomType(),
            doc.checkIn(),
            doc.checkOut(),
            doc.nights(),
            doc.subtotal(),
            doc.discount(),
            doc.total(),
            doc.text()
        );
    }
}
