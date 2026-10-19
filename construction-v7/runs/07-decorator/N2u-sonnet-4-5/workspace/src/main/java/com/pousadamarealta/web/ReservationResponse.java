package com.pousadamarealta.web;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

public class ReservationResponse {
    private long id;
    private int roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private String checkIn;
    private String checkOut;
    private int guests;
    private String ratePlan;
    private int nights;
    private double subtotal;
    private double discount;
    private double total;
    private String status;
    private double amountPaid;
    private Double refund;

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

        BigDecimal subtotalBd = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discountBd = reservation.calculateDiscount(subtotalBd);
        BigDecimal totalBd = reservation.calculateTotal(room.getNightlyRate());

        this.subtotal = subtotalBd.doubleValue();
        this.discount = discountBd.doubleValue();
        this.total = totalBd.doubleValue();
        this.status = reservation.getStatus().name();
        this.amountPaid = reservation.getAmountPaid().doubleValue();
        this.refund = reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null;
    }

    public long getId() {
        return id;
    }

    public int getRoomNumber() {
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

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public Double getRefund() {
        return refund;
    }
}
