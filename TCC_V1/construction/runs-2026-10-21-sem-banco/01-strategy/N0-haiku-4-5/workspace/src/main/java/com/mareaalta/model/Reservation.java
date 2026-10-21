package com.mareaalta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reservation {
    @JsonProperty("id")
    public int id;

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

    @JsonProperty("nights")
    public int nights;

    @JsonProperty("subtotal")
    public double subtotal;

    @JsonProperty("discount")
    public double discount;

    @JsonProperty("total")
    public double total;

    @JsonProperty("status")
    public String status;

    @JsonProperty("amountPaid")
    public double amountPaid;

    @JsonProperty("refund")
    public Double refund;

    public Reservation() {}

    public Reservation(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                      String checkIn, String checkOut, int guests, String ratePlan, String companyCode) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.companyCode = companyCode;
        this.status = "PENDING";
        this.amountPaid = 0;
        this.refund = null;
    }
}
