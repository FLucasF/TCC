package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class Reservation {
    private final long id;
    private final int roomNumber;
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

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
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
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(long id, int roomNumber, String guestName, String guestEmail,
                                      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests,
                                      RatePlan ratePlan, Pricing.Quote quote) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public void confirm() {
        move(EnumSet.of(ReservationStatus.PENDING), ReservationStatus.CONFIRMED);
        amountPaid = total;
    }

    public void checkIn() {
        move(EnumSet.of(ReservationStatus.CONFIRMED), ReservationStatus.CHECKED_IN);
    }

    public void checkOut() {
        move(EnumSet.of(ReservationStatus.CHECKED_IN), ReservationStatus.CHECKED_OUT);
    }

    public void cancel() {
        move(EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED), ReservationStatus.CANCELLED);
        refund = Money.round(ratePlan.refund(amountPaid));
    }

    private void move(Set<ReservationStatus> allowedFrom, ReservationStatus to) {
        if (!allowedFrom.contains(status)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserva " + id + " está " + status + " e não pode passar para " + to);
        }
        status = to;
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean occupies(LocalDate night) {
        return status.paid() && !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
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
}
