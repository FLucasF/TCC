package com.pousadamarealta.api;

import com.pousadamarealta.models.Reservation;
import java.math.BigDecimal;

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

    public static ReservationResponse fromReservation(Reservation res) {
        ReservationResponse resp = new ReservationResponse();
        resp.id = res.id;
        resp.roomNumber = res.roomNumber;
        resp.guestName = res.guestName;
        resp.guestEmail = res.guestEmail;
        resp.guestCpf = res.guestCpf;
        resp.checkIn = res.checkIn.toString();
        resp.checkOut = res.checkOut.toString();
        resp.guests = res.guests;
        resp.ratePlan = res.ratePlan.toString();
        resp.nights = res.nights;
        resp.subtotal = res.subtotal;
        resp.discount = res.discount;
        resp.total = res.total;
        resp.status = res.status.toString();
        resp.amountPaid = res.amountPaid;
        resp.refund = res.refund;
        return resp;
    }
}
