package com.pousada.marealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateReservationRequest {
    @JsonProperty("roomNumber")
    public Integer roomNumber;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestEmail")
    public String guestEmail;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("companyCnpj")
    public String companyCnpj;

    @JsonProperty("companyName")
    public String companyName;

    @JsonProperty("guestPassport")
    public String guestPassport;

    @JsonProperty("guestCountry")
    public String guestCountry;

    @JsonProperty("checkIn")
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("guests")
    public Integer guests;

    @JsonProperty("ratePlan")
    public String ratePlan;
}
