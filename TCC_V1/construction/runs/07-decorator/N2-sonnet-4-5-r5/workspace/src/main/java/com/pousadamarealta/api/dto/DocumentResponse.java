package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") double subtotal,
    @JsonProperty("discount") double discount,
    @JsonProperty("total") double total
) {}
