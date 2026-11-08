package com.pousada.marealta.api.response;

import com.pousada.marealta.model.Reservation;

public class ReservationResponse {
    public long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String checkIn;
    public String checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public String subtotal;
    public String discount;
    public String total;
    public String status;
    public String amountPaid;
    public String refund;

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
        this.subtotal = String.format("%.2f", reservation.getSubtotal());
        this.discount = String.format("%.2f", reservation.getDiscount());
        this.total = String.format("%.2f", reservation.getTotal());
        this.status = reservation.getStatus().name();
        this.amountPaid = String.format("%.2f", reservation.getAmountPaid());
        this.refund = reservation.getRefund() != null ?
                      String.format("%.2f", reservation.getRefund()) : null;
    }
}
