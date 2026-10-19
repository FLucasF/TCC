package com.pousada.domain;

import com.pousada.state.*;
import com.pousada.strategy.*;

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
    private final RatePlanStrategy ratePlanStrategy;

    private ReservationState state;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
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
        this.ratePlanStrategy = createRatePlanStrategy(ratePlan);
        this.state = new PendingState();
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    private RatePlanStrategy createRatePlanStrategy(RatePlan plan) {
        return switch (plan) {
            case STANDARD -> new StandardRatePlan();
            case NON_REFUNDABLE -> new NonRefundableRatePlan();
        };
    }

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal calculateNightlyRate(LocalDate date, BigDecimal baseRate) {
        if (isHighSeason(date)) {
            BigDecimal highSeasonRate = baseRate.multiply(new BigDecimal("1.20"));
            return highSeasonRate.setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate;
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(current, baseRate);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return ratePlanStrategy.calculateDiscount(subtotal);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public void confirm(BigDecimal amountPaid) {
        state.confirm(this, amountPaid);
    }

    public void cancel() {
        state.cancel(this);
    }

    public void checkIn() {
        state.checkIn(this);
    }

    public void checkOut() {
        state.checkOut(this);
    }

    public void setState(ReservationState state) {
        this.state = state;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public void setRefund(BigDecimal refund) {
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

    public RatePlanStrategy getRatePlanStrategy() {
        return ratePlanStrategy;
    }

    public ReservationStatus getStatus() {
        return state.getStatus();
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
