package com.pousada.marealta.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public ReservationStatus status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public Reservation() {}

    public Reservation(
        long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
        int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
        ReservationStatus status, BigDecimal amountPaid, BigDecimal refund
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
}
