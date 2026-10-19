package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.RatePlanType;

import java.time.LocalDate;

public record CreateReservationRequest(
        @JsonProperty("roomNumber") int roomNumber,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestEmail") String guestEmail,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("checkIn") LocalDate checkIn,
        @JsonProperty("checkOut") LocalDate checkOut,
        @JsonProperty("guests") int guests,
        @JsonProperty("ratePlan") RatePlanType ratePlan,
        @JsonProperty("companyCode") String companyCode
) {}
