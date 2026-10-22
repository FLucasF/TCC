package com.pousadamarealta.domain.model;

import com.pousadamarealta.domain.enums.RatePlan;
import com.pousadamarealta.domain.enums.ReservationStatus;

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
    private final BigDecimal nightlyRate;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                      BigDecimal nightlyRate, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
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

    public static Reservation create(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                    LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                    BigDecimal nightlyRate) {
        return new Reservation(null, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests, ratePlan, nightlyRate, ReservationStatus.PENDING, BigDecimal.ZERO, null);
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal getSubtotal() {
        return nightlyRate.multiply(new BigDecimal(getNights()))
                .setScale(2, RoundingMode.HALF_EVEN);
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

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = getTotal();
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        boolean wasConfirmed = status == ReservationStatus.CONFIRMED;
        this.status = ReservationStatus.CANCELLED;

        if (wasConfirmed && ratePlan == RatePlan.STANDARD) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
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

    public boolean isActive() {
        return status != ReservationStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }
}
