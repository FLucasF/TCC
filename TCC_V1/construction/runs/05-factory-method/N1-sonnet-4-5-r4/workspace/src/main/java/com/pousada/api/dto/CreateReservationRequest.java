package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateReservationRequest(
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    int guests,
    @JsonProperty("ratePlan") String ratePlan
) {}
