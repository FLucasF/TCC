package com.pousada.domain.model;

import com.pousada.domain.guest.GuestType;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {
    private long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final String guestName;
    private final String guestEmail;
    private final GuestType guestType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    private Reservation(Builder builder) {
        this.id = builder.id;
        this.roomNumber = builder.roomNumber;
        this.roomType = builder.roomType;
        this.guestName = builder.guestName;
        this.guestEmail = builder.guestEmail;
        this.guestType = builder.guestType;
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

    public long id() { return id; }
    public String roomNumber() { return roomNumber; }
    public RoomType roomType() { return roomType; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public GuestType guestType() { return guestType; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    public void setId(long id) {
        this.id = id;
    }

    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
    }

    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }
        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD && amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public void doCheckIn() {
        if (!canCheckIn()) {
            throw new IllegalStateException("Cannot check-in reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void doCheckOut() {
        if (!canCheckOut()) {
            throw new IllegalStateException("Cannot check-out reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public boolean canBeConfirmed() {
        return status == ReservationStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public boolean canCheckIn() {
        return status == ReservationStatus.CONFIRMED;
    }

    public boolean canCheckOut() {
        return status == ReservationStatus.CHECKED_IN;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
            || status == ReservationStatus.CHECKED_IN
            || status == ReservationStatus.CHECKED_OUT;
    }

    public Document generateDocument() {
        return guestType.generateDocument(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String roomNumber;
        private RoomType roomType;
        private String guestName;
        private String guestEmail;
        private GuestType guestType;
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
        public Builder roomNumber(String number) { this.roomNumber = number; return this; }
        public Builder roomType(RoomType type) { this.roomType = type; return this; }
        public Builder guestName(String name) { this.guestName = name; return this; }
        public Builder guestEmail(String email) { this.guestEmail = email; return this; }
        public Builder guestType(GuestType type) { this.guestType = type; return this; }
        public Builder checkIn(LocalDate date) { this.checkIn = date; return this; }
        public Builder checkOut(LocalDate date) { this.checkOut = date; return this; }
        public Builder guests(int guests) { this.guests = guests; return this; }
        public Builder ratePlan(RatePlan plan) { this.ratePlan = plan; return this; }
        public Builder nights(int nights) { this.nights = nights; return this; }
        public Builder subtotal(BigDecimal amount) { this.subtotal = amount; return this; }
        public Builder discount(BigDecimal amount) { this.discount = amount; return this; }
        public Builder total(BigDecimal amount) { this.total = amount; return this; }
        public Builder status(ReservationStatus status) { this.status = status; return this; }
        public Builder amountPaid(BigDecimal amount) { this.amountPaid = amount; return this; }
        public Builder refund(BigDecimal amount) { this.refund = amount; return this; }

        public Reservation build() {
            return new Reservation(this);
        }
    }
}
