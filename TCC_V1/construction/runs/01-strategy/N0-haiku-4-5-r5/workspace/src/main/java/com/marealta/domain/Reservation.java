package com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public int id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
    public String companyCode;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public Reservation() {}

    public Reservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                       String companyCode, int nights, BigDecimal subtotal, BigDecimal discount,
                       BigDecimal total) {
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
        this.status = "PENDING";
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }
}
