package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateReservationRequest(
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("guests") Integer guests,
    @JsonProperty("ratePlan") String ratePlan
) {}
