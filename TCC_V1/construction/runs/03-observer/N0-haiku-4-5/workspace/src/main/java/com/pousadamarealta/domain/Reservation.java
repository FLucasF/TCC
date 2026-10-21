package com.pousadamarealta.domain;

import java.time.LocalDate;

public class Reservation {
    public final long id;
    public final int roomNumber;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlan ratePlan;
    public final int nights;
    public final double subtotal;
    public final double discount;
    public final double total;
    public final ReservationStatus status;
    public final double amountPaid;
    public final Double refund;

    public Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        double subtotal,
        double discount,
        double total,
        ReservationStatus status,
        double amountPaid,
        Double refund
    ) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
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

    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
            ratePlan, nights, subtotal, discount, total, newStatus, amountPaid, refund
        );
    }

    public Reservation withAmountPaid(double amount) {
        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
            ratePlan, nights, subtotal, discount, total, status, amount, refund
        );
    }

    public Reservation withRefund(Double amount) {
        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
            ratePlan, nights, subtotal, discount, total, status, amountPaid, amount
        );
    }
}
