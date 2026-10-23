package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReservationResponse(
    @JsonProperty("id") long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("guests") int guests,
    @JsonProperty("ratePlan") String ratePlan,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") double subtotal,
    @JsonProperty("discount") double discount,
    @JsonProperty("total") double total,
    @JsonProperty("status") String status,
    @JsonProperty("amountPaid") double amountPaid,
    @JsonProperty("refund") @JsonInclude(JsonInclude.Include.CUSTOM) Double refund
) {}
