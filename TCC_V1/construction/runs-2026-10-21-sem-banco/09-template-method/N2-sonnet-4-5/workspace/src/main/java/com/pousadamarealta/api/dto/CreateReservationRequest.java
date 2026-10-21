package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record CreateReservationRequest(
        @JsonProperty("roomNumber") String roomNumber,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestEmail") String guestEmail,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("checkIn") LocalDate checkIn,
        @JsonProperty("checkOut") LocalDate checkOut,
        @JsonProperty("guests") int guests,
        @JsonProperty("ratePlan") String ratePlan
) {
}
