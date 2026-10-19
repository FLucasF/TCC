package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;

    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
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
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public void confirm(BigDecimal nightlyRate) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = calculateTotal(nightlyRate);
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can check in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can check out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;

        // Calcular reembolso
        if (ratePlan == RatePlan.STANDARD) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal calculateSubtotal(BigDecimal baseNightlyRate) {
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            double multiplier = SeasonCalculator.getSeasonMultiplier(date);
            BigDecimal nightPrice = baseNightlyRate.multiply(BigDecimal.valueOf(multiplier))
                    .setScale(2, RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(nightPrice);
            date = date.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return subtotal.multiply(ratePlan.getDiscountRate())
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal(BigDecimal baseNightlyRate) {
        BigDecimal subtotal = calculateSubtotal(baseNightlyRate);
        BigDecimal discount = calculateDiscount(subtotal);
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
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

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
