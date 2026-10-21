package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Reservation {
    private final long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final GuestIdentification identification;
    private final String guestEmail;
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

    public Reservation(long id, String roomNumber, RoomType roomType, GuestIdentification identification,
                      String guestEmail, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.identification = identification;
        this.guestEmail = guestEmail;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;

        this.nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = calculateSubtotal();
        this.discount = calculateDiscount();
        this.total = subtotal.subtract(discount);
    }

    private BigDecimal calculateSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            BigDecimal nightRate = roomType.getNightlyRate();
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            sum = sum.add(nightRate);
            date = date.plusDays(1);
        }

        return sum.setScale(2, RoundingMode.HALF_EVEN);
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

    private BigDecimal calculateDiscount() {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(new BigDecimal("0.15"))
                .setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = total;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot check in reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot check out reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }

        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean occupiesNight(LocalDate night) {
        return !status.equals(ReservationStatus.CANCELLED)
            && !night.isBefore(checkIn)
            && night.isBefore(checkOut);
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
            || status == ReservationStatus.CHECKED_IN
            || status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getNetAmount() {
        BigDecimal net = amountPaid;
        if (refund != null) {
            net = net.subtract(refund);
        }
        return net;
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

    public GuestIdentification getIdentification() {
        return identification;
    }

    public String getGuestName() {
        return identification.getGuestName();
    }

    public String getGuestEmail() {
        return guestEmail;
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

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
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
