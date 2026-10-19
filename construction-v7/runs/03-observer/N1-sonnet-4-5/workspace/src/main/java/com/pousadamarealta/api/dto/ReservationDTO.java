package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReservationDTO(
    long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    int guests,
    @JsonProperty("ratePlan") String ratePlan,
    int nights,
    double subtotal,
    double discount,
    double total,
    String status,
    @JsonProperty("amountPaid") double amountPaid,
    Double refund
) {
}
