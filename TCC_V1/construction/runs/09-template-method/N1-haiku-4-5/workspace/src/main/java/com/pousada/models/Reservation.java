package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private long id;
    private int roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
                       String guestCpf, LocalDate checkIn, LocalDate checkOut,
                       int guests, RatePlan ratePlan, int nights, BigDecimal subtotal,
                       BigDecimal discount, BigDecimal total, ReservationStatus status,
                       BigDecimal amountPaid, BigDecimal refund) {
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

    public long getId() {
        return id;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }

    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                newStatus, amountPaid, refund);
    }

    public Reservation withAmountPaid(BigDecimal newAmountPaid) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                status, newAmountPaid, refund);
    }

    public Reservation withRefund(BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                status, amountPaid, newRefund);
    }
}
