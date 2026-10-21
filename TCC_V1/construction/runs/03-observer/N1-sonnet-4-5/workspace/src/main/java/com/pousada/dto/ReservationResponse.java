package com.pousada.dto;

import com.pousada.domain.*;
import com.pousada.service.RoomService;

import java.math.BigDecimal;

public class ReservationResponse {
    private long id;
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

    public static ReservationResponse from(Reservation reservation, RoomService roomService) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        Money subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        Money discount = reservation.calculateDiscount(subtotal);
        Money total = reservation.calculateTotal(subtotal, discount);

        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn().toString();
        response.checkOut = reservation.getCheckOut().toString();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan();
        response.nights = reservation.getNights();
        response.subtotal = subtotal.getAmount().doubleValue();
        response.discount = discount.getAmount().doubleValue();
        response.total = total.getAmount().doubleValue();
        response.status = reservation.getStatus();
        response.amountPaid = reservation.getAmountPaid().getAmount().doubleValue();
        response.refund = reservation.getRefund() != null
            ? reservation.getRefund().getAmount().doubleValue()
            : null;

        return response;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public void setRatePlan(RatePlan ratePlan) {
        this.ratePlan = ratePlan;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Double getRefund() {
        return refund;
    }

    public void setRefund(Double refund) {
        this.refund = refund;
    }
}
