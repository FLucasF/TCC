package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Reservation;

import java.math.BigDecimal;

public class ReservationResponse {
    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final String checkIn;
    private final String checkOut;
    private final int guests;
    private final String ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String status;
    private final BigDecimal amountPaid;
    private final BigDecimal refund;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.roomNumber = reservation.getRoomNumber();
        this.guestName = reservation.getGuestName();
        this.guestEmail = reservation.getGuestEmail();
        this.guestCpf = reservation.getGuestCpf();
        this.checkIn = reservation.getCheckIn().toString();
        this.checkOut = reservation.getCheckOut().toString();
        this.guests = reservation.getGuests();
        this.ratePlan = reservation.getRatePlan().name();
        this.nights = reservation.getNights();
        this.subtotal = reservation.getSubtotal();
        this.discount = reservation.getDiscount();
        this.total = reservation.getTotal();
        this.status = reservation.getStatus().name();
        this.amountPaid = reservation.getAmountPaid();
        this.refund = reservation.getRefund();
    }

    public long getId() {
        return id;
    }

    @JsonProperty("roomNumber")
    public int getRoomNumber() {
        return roomNumber;
    }

    @JsonProperty("guestName")
    public String getGuestName() {
        return guestName;
    }

    @JsonProperty("guestEmail")
    public String getGuestEmail() {
        return guestEmail;
    }

    @JsonProperty("guestCpf")
    public String getGuestCpf() {
        return guestCpf;
    }

    @JsonProperty("checkIn")
    public String getCheckIn() {
        return checkIn;
    }

    @JsonProperty("checkOut")
    public String getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    @JsonProperty("ratePlan")
    public String getRatePlan() {
        return ratePlan;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    @JsonProperty("amountPaid")
    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
