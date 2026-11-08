package com.pousadamarealta.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class Reservation {
    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
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
    }

    public int getNights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public BigDecimal calculateSubtotal(BigDecimal nightlyRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            BigDecimal nightRate = nightlyRate;
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            subtotal = subtotal.add(nightRate);
            date = date.plusDays(1);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
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

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discountAmount = subtotal.multiply(ratePlan.getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
        return discountAmount;
    }

    public BigDecimal calculateTotal(BigDecimal nightlyRate) {
        BigDecimal subtotal = calculateSubtotal(nightlyRate);
        BigDecimal discount = calculateDiscount(subtotal);
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean canConfirm() {
        return status == ReservationStatus.PENDING;
    }

    public void confirm() {
        if (!canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = calculateTotal(null);
    }

    public void confirmWithTotal(BigDecimal total) {
        if (!canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = total;
    }

    public boolean canCheckIn() {
        return status == ReservationStatus.CONFIRMED;
    }

    public void checkIn() {
        if (!canCheckIn()) {
            throw new IllegalStateException("Cannot check in reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public boolean canCheckOut() {
        return status == ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (!canCheckOut()) {
            throw new IllegalStateException("Cannot check out reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public boolean canCancel() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }

        if (ratePlan == RatePlan.STANDARD && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }

        this.status = ReservationStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
