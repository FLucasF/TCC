package com.pousadamarealta.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;

import java.time.format.DateTimeFormatter;

public class ReservationResponse {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private String checkIn;
    private String checkOut;
    private int guests;
    private RatePlan ratePlan;
    private int nights;
    private double subtotal;
    private double discount;
    private double total;
    private ReservationStatus status;
    private double amountPaid;
    private Double refund;

    public static ReservationResponse from(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn().format(DATE_FORMATTER);
        response.checkOut = reservation.getCheckOut().format(DATE_FORMATTER);
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal().doubleValue();
        response.discount = reservation.getDiscount().doubleValue();
        response.total = reservation.getTotal().doubleValue();
        response.status = reservation.getStatus();
        response.amountPaid = reservation.getAmountPaid().doubleValue();
        response.refund = reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null;
        return response;
    }

    public Long getId() {
        return id;
    }

    @JsonProperty("roomNumber")
    public String getRoomNumber() {
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
    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public int getNights() {
        return nights;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    @JsonProperty("amountPaid")
    public double getAmountPaid() {
        return amountPaid;
    }

    public Double getRefund() {
        return refund;
    }
}
