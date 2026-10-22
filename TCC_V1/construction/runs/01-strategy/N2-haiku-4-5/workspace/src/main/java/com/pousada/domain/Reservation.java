package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    }

    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final String ratePlan;
    private final String companyCode;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final Status status;
    private final BigDecimal amountPaid;
    private final BigDecimal refund;

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
            String companyCode,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            Status status,
            BigDecimal amountPaid,
            BigDecimal refund
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
        this.companyCode = companyCode;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public String getRatePlan() { return ratePlan; }
    public String getCompanyCode() { return companyCode; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public Status getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }
}
