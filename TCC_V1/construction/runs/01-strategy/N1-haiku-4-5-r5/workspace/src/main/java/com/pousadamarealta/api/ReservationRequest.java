package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReservationRequest {
    @JsonProperty("roomNumber")
    public int roomNumber;

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
    public int guests;

    @JsonProperty("ratePlan")
    public String ratePlan;

    @JsonProperty("companyCode")
    public String companyCode;
}
