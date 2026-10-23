package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

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
    private BigDecimal subtotal;
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
        this.subtotal = BigDecimal.ZERO;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public int getNights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public BigDecimal calculateNightPrice(LocalDate date, BigDecimal baseRate) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            BigDecimal highSeasonMultiplier = new BigDecimal("1.20");
            price = price.multiply(highSeasonMultiplier).setScale(2, RoundingMode.HALF_EVEN);
        }
        return price;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(ratePlan.getDiscountRate()).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getTotal() {
        return subtotal.subtract(getDiscount());
    }

    public void confirm(BigDecimal subtotalValue, BigDecimal totalValue) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Can only confirm pending reservations");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.subtotal = subtotalValue;
        this.amountPaid = totalValue;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Can only check in confirmed reservations");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Can only check out checked-in reservations");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Can only cancel pending or confirmed reservations");
        }
        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
            this.amountPaid = BigDecimal.ZERO;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
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

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public void setRefund(BigDecimal refund) {
        this.refund = refund;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
