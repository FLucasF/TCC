package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentification identification;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final BigDecimal nightlyRate;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(String roomNumber, String guestName, String guestEmail,
                      GuestIdentification identification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, BigDecimal nightlyRate) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nightlyRate = nightlyRate;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = PricingCalculator.calculateSubtotal(nightlyRate, checkIn, checkOut);
        this.discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        this.total = PricingCalculator.calculateTotal(subtotal, discount);
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Can only confirm pending reservations");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
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
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean isActive() {
        return status != ReservationStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getRevenueAmount() {
        return amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund);
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

    public GuestIdentification getIdentification() {
        return identification;
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
}
