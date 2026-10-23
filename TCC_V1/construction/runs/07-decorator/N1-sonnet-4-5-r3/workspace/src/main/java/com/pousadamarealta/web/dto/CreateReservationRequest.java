package com.pousadamarealta.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.RatePlan;

import java.time.LocalDate;

public record CreateReservationRequest(
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    int guests,
    @JsonProperty("ratePlan") RatePlan ratePlan
) {}
