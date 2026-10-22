package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.model.Document;

import java.math.BigDecimal;

public record DocumentResponse(
    String title,
    @JsonProperty("reservationId") Long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
            document.title(),
            document.reservationId(),
            document.guestName(),
            document.guestCpf(),
            document.roomNumber(),
            document.roomType().name(),
            document.checkIn().toString(),
            document.checkOut().toString(),
            document.nights(),
            document.subtotal(),
            document.discount(),
            document.total()
        );
    }
}
