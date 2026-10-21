package com.pousada.dto;

import com.google.gson.annotations.SerializedName;

public class CreateReservationRequest {
    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("guestName")
    public String guestName;

    @SerializedName("guestEmail")
    public String guestEmail;

    @SerializedName("guestCpf")
    public String guestCpf;

    @SerializedName("companyCnpj")
    public String companyCnpj;

    @SerializedName("companyName")
    public String companyName;

    @SerializedName("guestPassport")
    public String guestPassport;

    @SerializedName("guestCountry")
    public String guestCountry;

    @SerializedName("checkIn")
    public String checkIn;

    @SerializedName("checkOut")
    public String checkOut;

    @SerializedName("guests")
    public int guests;

    @SerializedName("ratePlan")
    public String ratePlan;
}
