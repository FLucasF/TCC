package com.pousada.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public final long id;
    public final int roomNumber;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final String companyCnpj;
    public final String companyName;
    public final String guestPassport;
    public final String guestCountry;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final String ratePlan;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;
    public final String status;
    public final BigDecimal amountPaid;
    public final BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
                      String guestCpf, String companyCnpj, String companyName,
                      String guestPassport, String guestCountry,
                      LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                      int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                      String status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public Reservation withStatus(String newStatus) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
                subtotal, discount, total, newStatus, amountPaid, refund);
    }

    public Reservation withAmountPaid(BigDecimal amount) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
                subtotal, discount, total, status, amount, refund);
    }

    public Reservation withRefund(BigDecimal amount) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
                subtotal, discount, total, status, amountPaid, amount);
    }
}
