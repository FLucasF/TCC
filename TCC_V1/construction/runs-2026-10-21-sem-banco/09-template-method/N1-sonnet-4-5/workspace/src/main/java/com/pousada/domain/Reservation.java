package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
    private final long id;
    private final String roomNumber;
    private final RoomType roomType;
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

    public Reservation(long id, String roomNumber, RoomType roomType, String guestName, String guestEmail,
                      String guestCpf, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, BigDecimal nightlyRate) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
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

    public long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
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

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal getSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            BigDecimal rate = nightlyRate;
            if (isHighSeason(date)) {
                BigDecimal surcharge = rate.multiply(new BigDecimal("0.20"));
                rate = rate.add(surcharge).setScale(2, RoundingMode.HALF_EVEN);
            }
            total = total.add(rate);
            date = date.plusDays(1);
        }

        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getDiscount() {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = getSubtotal();
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getTotal() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal discount = getDiscount();
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public List<LocalDate> getOccupiedNights() {
        List<LocalDate> nights = new ArrayList<>();
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            nights.add(date);
            date = date.plusDays(1);
        }
        return nights;
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
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

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = getTotal();
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

        if (ratePlan.isRefundable()) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getNetAmount() {
        BigDecimal net = amountPaid;
        if (refund != null) {
            net = net.subtract(refund);
        }
        return net;
    }
}
