package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.RatePlanType;

import java.time.LocalDate;

public record CreateReservationRequest(
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    @JsonProperty("guests") Integer guests,
    @JsonProperty("ratePlan") RatePlanType ratePlan,
    @JsonProperty("companyCode") String companyCode
) {}
