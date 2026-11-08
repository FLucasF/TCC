package com.marealata.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    @SerializedName("id")
    public Long id;

    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("guestName")
    public String guestName;

    @SerializedName("guestEmail")
    public String guestEmail;

    @SerializedName("guestCpf")
    public String guestCpf;

    @SerializedName("checkIn")
    public LocalDate checkIn;

    @SerializedName("checkOut")
    public LocalDate checkOut;

    @SerializedName("guests")
    public int guests;

    @SerializedName("ratePlan")
    public RatePlan ratePlan;

    @SerializedName("nights")
    public int nights;

    @SerializedName("subtotal")
    public BigDecimal subtotal;

    @SerializedName("discount")
    public BigDecimal discount;

    @SerializedName("total")
    public BigDecimal total;

    @SerializedName("status")
    public ReservationStatus status;

    @SerializedName("amountPaid")
    public BigDecimal amountPaid;

    @SerializedName("refund")
    public BigDecimal refund;

    public Reservation() {}

    public Reservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }
}
