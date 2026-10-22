package com.pousadamaralta.domain.model;

import com.pousadamaralta.domain.identification.GuestIdentification;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final Long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentification identification;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlanType ratePlanType;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(
            Long id,
            String roomNumber,
            String guestName,
            String guestEmail,
            GuestIdentification identification,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlanType ratePlanType,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            ReservationStatus status,
            BigDecimal amountPaid,
            BigDecimal refund
    ) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlanType = ratePlanType;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public boolean canConfirm() {
        return status == ReservationStatus.PENDING;
    }

    public void confirm() {
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

    public void cancel(BigDecimal refundAmount) {
        if (!canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }
        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
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

    public RatePlanType getRatePlanType() {
        return ratePlanType;
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
