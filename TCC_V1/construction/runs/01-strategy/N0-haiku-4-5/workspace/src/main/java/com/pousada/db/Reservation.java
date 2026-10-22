package com.pousada.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    public LocalDate checkIn;

    @JsonProperty("checkOut")
    public LocalDate checkOut;

    @JsonProperty("guests")
    public int guests;

    @JsonProperty("ratePlan")
    public String ratePlan;

    @JsonProperty("nights")
    public int nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    @JsonProperty("status")
    public String status;

    @JsonProperty("amountPaid")
    public BigDecimal amountPaid;

    @JsonProperty("refund")
    public BigDecimal refund;

    public Reservation(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                      int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                      String status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
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
