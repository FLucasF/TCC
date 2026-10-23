package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Reservation {
    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentification guestIdentification;
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

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
                      GuestIdentification guestIdentification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, BigDecimal nightlyRate) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestIdentification = guestIdentification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = calculateSubtotal(nightlyRate);
        this.discount = calculateDiscount(this.subtotal);
        this.total = this.subtotal.subtract(this.discount);
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
                      GuestIdentification guestIdentification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total, ReservationStatus status,
                      BigDecimal amountPaid, BigDecimal refund) {
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
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    private BigDecimal calculateSubtotal(BigDecimal nightlyRate) {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal rate = nightlyRate;
            if (isHighSeason(current)) {
                rate = rate.multiply(new BigDecimal("1.20"))
                          .setScale(2, RoundingMode.HALF_EVEN);
            }
            sum = sum.add(rate);
            current = current.plusDays(1);
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 12 && day >= 15) return true;
        if (month == 1 || month == 2) return true;
        if (month == 7) return true;

        return false;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (ratePlan.discountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(ratePlan.discountRate())
                      .setScale(2, RoundingMode.HALF_EVEN);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }
        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public void performCheckIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void performCheckOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can be checked out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public long id() { return id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public GuestIdentification guestIdentification() { return guestIdentification; }
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
}
