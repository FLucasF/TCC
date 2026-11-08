package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.models.Reservation;
import java.math.BigDecimal;

public class ReservationResponse {
    @JsonProperty("id")
    public final int id;

    @JsonProperty("roomNumber")
    public final int roomNumber;

    @JsonProperty("guestName")
    public final String guestName;

    @JsonProperty("guestEmail")
    public final String guestEmail;

    @JsonProperty("guestCpf")
    public final String guestCpf;

    @JsonProperty("checkIn")
    public final String checkIn;

    @JsonProperty("checkOut")
    public final String checkOut;

    @JsonProperty("guests")
    public final int guests;

    @JsonProperty("ratePlan")
    public final String ratePlan;

    @JsonProperty("nights")
    public final int nights;

    @JsonProperty("subtotal")
    public final BigDecimal subtotal;

    @JsonProperty("discount")
    public final BigDecimal discount;

    @JsonProperty("total")
    public final BigDecimal total;

    @JsonProperty("status")
    public final String status;

    @JsonProperty("amountPaid")
    public final BigDecimal amountPaid;

    @JsonProperty("refund")
    public final BigDecimal refund;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.id;
        this.roomNumber = reservation.roomNumber;
        this.guestName = reservation.guestName;
        this.guestEmail = reservation.guestEmail;
        this.guestCpf = reservation.guestCpf;
        this.checkIn = reservation.checkIn.toString();
        this.checkOut = reservation.checkOut.toString();
        this.guests = reservation.guests;
        this.ratePlan = reservation.ratePlan;
        this.nights = reservation.nights;
        this.subtotal = reservation.subtotal;
        this.discount = reservation.discount;
        this.total = reservation.total;
        this.status = reservation.status;
        this.amountPaid = reservation.amountPaid;
        this.refund = reservation.refund;
    }
}
