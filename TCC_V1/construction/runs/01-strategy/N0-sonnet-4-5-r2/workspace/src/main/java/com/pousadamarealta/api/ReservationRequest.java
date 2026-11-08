package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.RatePlan;

import java.time.LocalDate;

public record ReservationRequest(
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    @JsonProperty("guests") Integer guests,
    @JsonProperty("ratePlan") RatePlan ratePlan,
    @JsonProperty("companyCode") String companyCode
) {
}
