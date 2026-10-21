package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.model.Reservation;
import com.pousada.model.RatePlanType;
import com.pousada.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    @JsonProperty("id")
    public long id;

    @JsonProperty("roomNumber")
    public String roomNumber;

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
    public RatePlanType ratePlan;

    @JsonProperty("nights")
    public long nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    @JsonProperty("status")
    public ReservationStatus status;

    @JsonProperty("amountPaid")
    public BigDecimal amountPaid;

    @JsonProperty("refund")
    public BigDecimal refund;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.roomNumber = reservation.getRoomNumber();
        this.guestName = reservation.getGuestName();
        this.guestEmail = reservation.getGuestEmail();
        this.guestCpf = reservation.getGuestCpf();
        this.checkIn = reservation.getCheckIn();
        this.checkOut = reservation.getCheckOut();
        this.guests = reservation.getGuests();
        this.ratePlan = reservation.getRatePlan();
        this.nights = reservation.getNights();
        this.subtotal = reservation.getSubtotal();
        this.discount = reservation.getDiscount();
        this.total = reservation.getTotal();
        this.status = reservation.getStatus();
        this.amountPaid = reservation.getAmountPaid();
        this.refund = reservation.getRefund();
    }
}
