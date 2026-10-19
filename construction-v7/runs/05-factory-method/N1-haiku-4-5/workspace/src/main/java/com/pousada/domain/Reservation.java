package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Reservation {
    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    }

    public enum RatePlan {
        STANDARD, NON_REFUNDABLE
    }

    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentification guestIdentification;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final long nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private Status status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
                      GuestIdentification guestIdentification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, long nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestIdentification = guestIdentification;
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

    public long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public GuestIdentification getGuestIdentification() { return guestIdentification; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public long getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public Status getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }

    public void confirm(BigDecimal amountPaid) {
        Objects.requireNonNull(amountPaid);
        if (status != Status.PENDING) {
            throw new IllegalStateException("Can only confirm a PENDING reservation");
        }
        this.status = Status.CONFIRMED;
        this.amountPaid = amountPaid;
    }

    public void checkIn() {
        if (status != Status.CONFIRMED) {
            throw new IllegalStateException("Can only check in a CONFIRMED reservation");
        }
        this.status = Status.CHECKED_IN;
    }

    public void checkOut() {
        if (status != Status.CHECKED_IN) {
            throw new IllegalStateException("Can only check out a CHECKED_IN reservation");
        }
        this.status = Status.CHECKED_OUT;
    }

    public void cancel() {
        if (status != Status.PENDING && status != Status.CONFIRMED) {
            throw new IllegalStateException("Can only cancel PENDING or CONFIRMED reservations");
        }

        if (status == Status.CONFIRMED) {
            if (ratePlan == RatePlan.STANDARD) {
                this.refund = amountPaid;
            } else {
                this.refund = BigDecimal.ZERO;
            }
        } else {
            this.refund = BigDecimal.ZERO;
        }

        this.status = Status.CANCELLED;
    }
}
