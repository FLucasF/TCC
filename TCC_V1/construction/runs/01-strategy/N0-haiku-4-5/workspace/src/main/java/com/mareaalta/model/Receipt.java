package com.mareaalta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Receipt {
    @JsonProperty("title")
    public String title = "Comprovante de reserva";

    @JsonProperty("reservationId")
    public int reservationId;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("roomNumber")
    public int roomNumber;

    @JsonProperty("roomType")
    public String roomType;

    @JsonProperty("checkIn")
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("nights")
    public int nights;

    @JsonProperty("subtotal")
    public double subtotal;

    @JsonProperty("discount")
    public double discount;

    @JsonProperty("total")
    public double total;

    public Receipt(int reservationId, String guestName, String guestCpf, int roomNumber,
                  String roomType, String checkIn, String checkOut, int nights,
                  double subtotal, double discount, double total) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }
}
