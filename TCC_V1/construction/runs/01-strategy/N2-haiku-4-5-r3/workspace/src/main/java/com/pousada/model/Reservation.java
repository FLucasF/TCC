package com.pousada.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public final int id;
    public final int roomNumber;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlanType ratePlan;
    public final String companyCode; // For CORPORATE plan
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;
    public final ReservationStatus status;
    public final BigDecimal amountPaid;
    public final BigDecimal refund;

    public Reservation(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlanType ratePlan,
                       String companyCode, int nights, BigDecimal subtotal, BigDecimal discount,
                       BigDecimal total, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.companyCode = companyCode;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }
}
