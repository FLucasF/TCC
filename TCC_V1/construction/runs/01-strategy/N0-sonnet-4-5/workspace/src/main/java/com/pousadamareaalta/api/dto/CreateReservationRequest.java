package com.pousadamareaalta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateReservationRequest(
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan,
    @JsonProperty("companyCode") String companyCode
) {
}
