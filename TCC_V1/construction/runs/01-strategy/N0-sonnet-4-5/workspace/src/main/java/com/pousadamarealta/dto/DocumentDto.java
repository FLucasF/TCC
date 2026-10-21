package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record DocumentDto(
        @JsonProperty("title") String title,
        @JsonProperty("reservationId") Long reservationId,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("roomNumber") String roomNumber,
        @JsonProperty("roomType") String roomType,
        @JsonProperty("checkIn") String checkIn,
        @JsonProperty("checkOut") String checkOut,
        @JsonProperty("nights") int nights,
        @JsonProperty("subtotal") BigDecimal subtotal,
        @JsonProperty("discount") BigDecimal discount,
        @JsonProperty("total") BigDecimal total
) {
}
