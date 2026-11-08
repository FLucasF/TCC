package com.pousada.marealta.domain;

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

    private Reservation(Builder builder) {
        this.id = builder.id;
        this.roomNumber = builder.roomNumber;
        this.guestName = builder.guestName;
        this.guestEmail = builder.guestEmail;
        this.guestCpf = builder.guestCpf;
        this.checkIn = builder.checkIn;
        this.checkOut = builder.checkOut;
        this.guests = builder.guests;
        this.ratePlan = builder.ratePlan;
        this.nights = builder.nights;
        this.subtotal = builder.subtotal;
        this.discount = builder.discount;
        this.total = builder.total;
        this.status = builder.status;
        this.amountPaid = builder.amountPaid;
        this.refund = builder.refund;
    }

    // Getters
    public long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }

    public static class Builder {
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
        private ReservationStatus status = ReservationStatus.PENDING;
        private BigDecimal amountPaid = BigDecimal.ZERO;
        private BigDecimal refund;

        public Builder id(long id) { this.id = id; return this; }
        public Builder roomNumber(int roomNumber) { this.roomNumber = roomNumber; return this; }
        public Builder guestName(String guestName) { this.guestName = guestName; return this; }
        public Builder guestEmail(String guestEmail) { this.guestEmail = guestEmail; return this; }
        public Builder guestCpf(String guestCpf) { this.guestCpf = guestCpf; return this; }
        public Builder checkIn(LocalDate checkIn) { this.checkIn = checkIn; return this; }
        public Builder checkOut(LocalDate checkOut) { this.checkOut = checkOut; return this; }
        public Builder guests(int guests) { this.guests = guests; return this; }
        public Builder ratePlan(RatePlan ratePlan) { this.ratePlan = ratePlan; return this; }
        public Builder nights(int nights) { this.nights = nights; return this; }
        public Builder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public Builder discount(BigDecimal discount) { this.discount = discount; return this; }
        public Builder total(BigDecimal total) { this.total = total; return this; }
        public Builder status(ReservationStatus status) { this.status = status; return this; }
        public Builder amountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; return this; }
        public Builder refund(BigDecimal refund) { this.refund = refund; return this; }

        public Reservation build() {
            return new Reservation(this);
        }
    }

    public enum RatePlan {
        STANDARD, NON_REFUNDABLE
    }

    public enum ReservationStatus {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    }
}
