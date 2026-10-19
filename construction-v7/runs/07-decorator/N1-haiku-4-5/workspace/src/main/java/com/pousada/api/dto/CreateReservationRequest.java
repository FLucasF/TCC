package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.models.RatePlan;
import java.time.LocalDate;

public record CreateReservationRequest(
        @JsonProperty("roomNumber")
        Integer roomNumber,
        @JsonProperty("guestName")
        String guestName,
        @JsonProperty("guestEmail")
        String guestEmail,
        @JsonProperty("guestCpf")
        String guestCpf,
        @JsonProperty("checkIn")
        LocalDate checkIn,
        @JsonProperty("checkOut")
        LocalDate checkOut,
        @JsonProperty("guests")
        Integer guests,
        @JsonProperty("ratePlan")
        RatePlan ratePlan
) {}
