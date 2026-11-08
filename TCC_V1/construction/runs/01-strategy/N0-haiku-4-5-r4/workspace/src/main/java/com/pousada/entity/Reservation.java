package com.pousada.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public final int id;
    public final int roomNumber;
    public final String roomType;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final String ratePlan;
    public final String companyCode;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;
    public final String status;
    public final BigDecimal amountPaid;
    public final BigDecimal refund;

    public Reservation(int id, int roomNumber, String roomType, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode,
                      int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                      String status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
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
