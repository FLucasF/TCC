package com.pousadamareal.domain;

import java.time.LocalDate;

public class Reservation {
    public long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public long subtotalCents;
    public long discountCents;
    public long totalCents;
    public ReservationStatus status;
    public long amountPaidCents;
    public Long refundCents;

    public enum ReservationStatus {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    }

    public Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        int nights,
        long subtotalCents,
        long discountCents,
        long totalCents
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
        this.subtotalCents = subtotalCents;
        this.discountCents = discountCents;
        this.totalCents = totalCents;
        this.status = ReservationStatus.PENDING;
        this.amountPaidCents = 0;
        this.refundCents = null;
    }
}
