package com.pousadamarealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    }

    public enum RatePlan {
        STANDARD, NON_REFUNDABLE
    }

    public final int id;
    public final int roomNumber;
    public final String guestName;
    public final String guestEmail;
    public final GuestIdentification identification;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlan ratePlan;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;
    public Status status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public Reservation(int id, int roomNumber, String guestName, String guestEmail,
                      GuestIdentification identification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = Status.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }
}
