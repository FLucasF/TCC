package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final Long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlanType ratePlanType;
    private final String companyCode;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlanType ratePlanType,
                      String companyCode, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlanType = ratePlanType;
        this.companyCode = companyCode;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
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

    public void cancel(BigDecimal refundAmount) {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Can only cancel pending or confirmed reservations");
        }
        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;
    }

    public boolean isActive() {
        return status != ReservationStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
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

    public RatePlanType getRatePlanType() {
        return ratePlanType;
    }

    public String getCompanyCode() {
        return companyCode;
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
