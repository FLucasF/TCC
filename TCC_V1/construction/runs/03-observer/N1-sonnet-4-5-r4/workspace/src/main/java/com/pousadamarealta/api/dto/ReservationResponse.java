package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;

public class ReservationResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("roomNumber")
    private String roomNumber;

    @JsonProperty("guestName")
    private String guestName;

    @JsonProperty("guestEmail")
    private String guestEmail;

    @JsonProperty("guestCpf")
    private String guestCpf;

    @JsonProperty("checkIn")
    private String checkIn;

    @JsonProperty("checkOut")
    private String checkOut;

    @JsonProperty("guests")
    private int guests;

    @JsonProperty("ratePlan")
    private String ratePlan;

    @JsonProperty("nights")
    private int nights;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("discount")
    private BigDecimal discount;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amountPaid")
    private BigDecimal amountPaid;

    @JsonProperty("refund")
    private BigDecimal refund;

    public ReservationResponse(Reservation reservation, Room room) {
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
        this.subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        this.discount = reservation.calculateDiscount(this.subtotal);
        this.total = this.subtotal.subtract(this.discount);
        this.status = reservation.getStatus().name();
        this.amountPaid = reservation.getAmountPaid();
        this.refund = reservation.getRefund();
    }

    public Long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
