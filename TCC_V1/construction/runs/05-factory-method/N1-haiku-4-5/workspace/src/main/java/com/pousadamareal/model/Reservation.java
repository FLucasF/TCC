package com.pousadamareal.model;

import com.google.gson.annotations.SerializedName;

public class Reservation {
    @SerializedName("id")
    public int id;

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
    public RatePlan ratePlan;

    @SerializedName("nights")
    public int nights;

    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount")
    public double discount;

    @SerializedName("total")
    public double total;

    @SerializedName("status")
    public ReservationStatus status;

    @SerializedName("amountPaid")
    public double amountPaid;

    @SerializedName("refund")
    public Double refund;

    public Reservation() {}

    public Reservation(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                      String companyCnpj, String companyName, String guestPassport, String guestCountry,
                      String checkIn, String checkOut, int guests, RatePlan ratePlan, int nights,
                      double subtotal, double discount, double total, ReservationStatus status,
                      double amountPaid, Double refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }
}
