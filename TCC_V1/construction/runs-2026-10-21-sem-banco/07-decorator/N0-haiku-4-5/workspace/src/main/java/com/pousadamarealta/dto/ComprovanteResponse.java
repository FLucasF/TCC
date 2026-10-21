package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComprovanteResponse {
    @JsonProperty("title")
    public String title = "Comprovante de reserva";

    @JsonProperty("reservationId")
    public long reservationId;

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
}
