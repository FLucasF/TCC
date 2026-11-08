package com.pousadamarealta.domain;

import com.pousadamarealta.notification.ReservationObserver;
import com.pousadamarealta.util.SeasonCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    private Long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
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

    private final List<ReservationObserver> observers = new ArrayList<>();

    public Reservation(String roomNumber, RoomType roomType, String guestName, String guestEmail,
                      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;

        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = calculateSubtotal();
        this.discount = calculateDiscount();
        this.total = subtotal.subtract(discount);
    }

    private BigDecimal calculateSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightRate = roomType.getNightlyRate();

            if (SeasonCalculator.isHighSeason(currentDate)) {
                nightRate = nightRate.multiply(HIGH_SEASON_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_EVEN);
            }

            sum = sum.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateDiscount() {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return subtotal.multiply(ratePlan.getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    public void addObserver(ReservationObserver observer) {
        observers.add(observer);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }

        ReservationStatus previousStatus = this.status;
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = total;

        notifyConfirmed();
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

        boolean wasConfirmed = (status == ReservationStatus.CONFIRMED);

        if (wasConfirmed && ratePlan.isRefundable()) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }

        this.status = ReservationStatus.CANCELLED;

        notifyCancelled(wasConfirmed);
    }

    private void notifyConfirmed() {
        for (ReservationObserver observer : observers) {
            observer.onReservationConfirmed(this);
        }
    }

    private void notifyCancelled(boolean wasConfirmed) {
        for (ReservationObserver observer : observers) {
            observer.onReservationCancelled(this, wasConfirmed);
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

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }

    public void setRefund(BigDecimal refund) {
        this.refund = refund;
    }

    public List<ReservationObserver> getObservers() {
        return observers;
    }
}
