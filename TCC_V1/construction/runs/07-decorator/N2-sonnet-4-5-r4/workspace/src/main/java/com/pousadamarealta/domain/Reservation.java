package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
        this.refund = null;
    }

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal getSubtotal(BigDecimal nightlyRate) {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            BigDecimal nightPrice = nightlyRate;
            if (SeasonCalculator.isHighSeason(date)) {
                nightPrice = nightPrice.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            sum = sum.add(nightPrice);
            date = date.plusDays(1);
        }

        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getDiscount(BigDecimal subtotal) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(new BigDecimal("0.15"))
                .setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotal(BigDecimal nightlyRate) {
        BigDecimal subtotal = getSubtotal(nightlyRate);
        BigDecimal discount = getDiscount(subtotal);
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public void confirm(BigDecimal nightlyRate) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = getTotal(nightlyRate);
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can be checked out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
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
