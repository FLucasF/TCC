package com.pousada.marealta.api;

import java.math.BigDecimal;
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
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.id;
        this.roomNumber = reservation.roomNumber;
        this.guestName = reservation.guestName;
        this.guestEmail = reservation.guestEmail;
        this.guestCpf = reservation.guestCpf;
        this.checkIn = reservation.checkIn.toString();
        this.checkOut = reservation.checkOut.toString();
        this.guests = reservation.guests;
        this.ratePlan = reservation.ratePlan.toString();
        this.nights = reservation.nights;
        this.subtotal = reservation.subtotal;
        this.discount = reservation.discount;
        this.total = reservation.total;
        this.status = reservation.status.toString();
        this.amountPaid = reservation.amountPaid;
        this.refund = reservation.refund;
    }
}
