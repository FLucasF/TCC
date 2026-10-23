package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final BigDecimal nightlyRate;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       BigDecimal nightlyRate) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nightlyRate = nightlyRate;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       BigDecimal nightlyRate, ReservationStatus status, BigDecimal amountPaid,
                       BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nightlyRate = nightlyRate;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal getSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = nightlyRate;
            if (isHighSeason(current)) {
                nightPrice = nightPrice.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            sum = sum.add(nightPrice);
            current = current.plusDays(1);
        }

        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getDiscount() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
        return discount;
    }

    public BigDecimal getTotal() {
        return getSubtotal().subtract(getDiscount())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public void confirm() {
        if (!status.canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = getTotal();
    }

    public void checkIn() {
        if (!status.canCheckIn()) {
            throw new IllegalStateException("Cannot check-in reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new IllegalStateException("Cannot check-out reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (!status.canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }

        this.status = ReservationStatus.CANCELLED;

        if (ratePlan.isRefundable() && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean occupiesNight(LocalDate night) {
        return !status.isActive() ? false :
               !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public Long getId() {
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

    public BigDecimal getNightlyRate() {
        return nightlyRate;
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
