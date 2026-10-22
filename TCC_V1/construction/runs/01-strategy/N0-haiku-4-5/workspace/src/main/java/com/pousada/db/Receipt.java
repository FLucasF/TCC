package com.pousada.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Receipt {
    @JsonProperty("title")
    public String title;

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
    public LocalDate checkIn;

    @JsonProperty("checkOut")
    public LocalDate checkOut;

    @JsonProperty("nights")
    public int nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    public Receipt(int reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                   LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.title = "Comprovante de reserva";
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
