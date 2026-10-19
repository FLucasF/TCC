package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.RatePlan;

public class CreateReservationRequest {
    @JsonProperty("roomNumber")
    public String roomNumber;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestEmail")
    public String guestEmail;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("checkIn")
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("guests")
    public Integer guests;

    @JsonProperty("ratePlan")
    public RatePlan ratePlan;
}
