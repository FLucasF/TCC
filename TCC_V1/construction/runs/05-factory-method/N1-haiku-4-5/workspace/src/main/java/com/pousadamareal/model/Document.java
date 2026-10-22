package com.pousadamareal.model;

import com.google.gson.annotations.SerializedName;

public class Document {
    @SerializedName("title")
    public String title;

    @SerializedName("reservationId")
    public int reservationId;

    @SerializedName("guestName")
    public String guestName;

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

    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("roomType")
    public String roomType;

    @SerializedName("checkIn")
    public String checkIn;

    @SerializedName("checkOut")
    public String checkOut;

    @SerializedName("nights")
    public int nights;

    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount")
    public double discount;

    @SerializedName("total")
    public double total;

    @SerializedName("text")
    public String text;

    public Document() {}

    public Document(String title, int reservationId, String guestName, String guestCpf,
                   String companyCnpj, String companyName, String guestPassport, String guestCountry,
                   int roomNumber, String roomType, String checkIn, String checkOut, int nights,
                   double subtotal, double discount, double total, String text) {
        this.title = title;
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.text = text;
    }
}
