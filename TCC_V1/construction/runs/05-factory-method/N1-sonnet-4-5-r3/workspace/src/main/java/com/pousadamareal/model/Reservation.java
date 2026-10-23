package com.pousadamareal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentification identification;
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

    public Reservation(long id, String roomNumber, String guestName, String guestEmail,
                      GuestIdentification identification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public Reservation(long id, String roomNumber, String guestName, String guestEmail,
                      GuestIdentification identification, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlan ratePlan, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total, ReservationStatus status,
                      BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public long getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public GuestIdentification getIdentification() { return identification; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
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

        if (ratePlan == RatePlan.STANDARD) {
            this.refund = this.amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public Document generateDocument(RoomType roomType) {
        return identification.generateDocument(
            id, guestName, roomNumber, roomType,
            checkIn, checkOut, nights,
            subtotal, discount, total
        );
    }
}
