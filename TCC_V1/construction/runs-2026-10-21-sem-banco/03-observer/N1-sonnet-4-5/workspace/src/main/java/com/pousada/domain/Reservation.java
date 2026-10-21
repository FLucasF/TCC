package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class Reservation {
    private final long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;

    private ReservationStatus status;
    private Money amountPaid;
    private Money refund;

    public Reservation(long id, String roomNumber, String guestName, String guestEmail,
                      String guestCpf, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = new Money(BigDecimal.ZERO);
        this.refund = null;
    }

    public int getNights() {
        return (int) checkIn.until(checkOut).getDays();
    }

    public Money calculateSubtotal(Money nightlyRate) {
        Money total = new Money(BigDecimal.ZERO);
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            Money rate = nightlyRate;
            if (isHighSeason(current)) {
                rate = rate.multiply(new BigDecimal("1.20"));
            }
            total = total.add(rate);
            current = current.plusDays(1);
        }

        return total;
    }

    public Money calculateDiscount(Money subtotal) {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return new Money(BigDecimal.ZERO);
        }
        return subtotal.multiply(ratePlan.getDiscountRate());
    }

    public Money calculateTotal(Money subtotal, Money discount) {
        return subtotal.subtract(discount);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.JULY) {
            return true;
        }

        return false;
    }

    public boolean canConfirm() {
        return status == ReservationStatus.PENDING;
    }

    public boolean canCheckIn() {
        return status == ReservationStatus.CONFIRMED;
    }

    public boolean canCheckOut() {
        return status == ReservationStatus.CHECKED_IN;
    }

    public boolean canCancel() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public void confirm(Money amount) {
        if (!canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = amount;
    }

    public void checkIn() {
        if (!canCheckIn()) {
            throw new IllegalStateException("Cannot check in reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (!canCheckOut()) {
            throw new IllegalStateException("Cannot check out reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public Money cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }

        Money refundAmount = new Money(BigDecimal.ZERO);

        if (status == ReservationStatus.CONFIRMED && ratePlan == RatePlan.STANDARD) {
            refundAmount = amountPaid;
        }

        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;

        return refundAmount;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
            || status == ReservationStatus.CHECKED_IN
            || status == ReservationStatus.CHECKED_OUT;
    }

    public boolean occupiesNight(LocalDate night) {
        return !checkIn.isAfter(night) && checkOut.isAfter(night);
    }

    public long getId() {
        return id;
    }

    public String getRoomNumber() {
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

    public ReservationStatus getStatus() {
        return status;
    }

    public Money getAmountPaid() {
        return amountPaid;
    }

    public Money getRefund() {
        return refund;
    }
}
