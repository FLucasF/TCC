package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {
    private final Long id;
    private final int roomNumber;
    private final Guest guest;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests,
                       RatePlan ratePlan, Quote quote, ReservationStatus status, BigDecimal amountPaid,
                       BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guest = guest;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(Room room, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests,
                                      RatePlan ratePlan) {
        Quote quote = Quote.of(room, checkIn, checkOut, ratePlan);
        return new Reservation(null, room.number(), guest, checkIn, checkOut, guests, ratePlan, quote,
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED);
        amountPaid = quote.total();
    }

    public void checkIn() {
        moveTo(ReservationStatus.CHECKED_IN);
    }

    public void checkOut() {
        moveTo(ReservationStatus.CHECKED_OUT);
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED);
        refund = ratePlan.refundFor(amountPaid);
    }

    private void moveTo(ReservationStatus target) {
        if (!target.canFollow(status)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva na situação " + status);
        }
        status = target;
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public Long id() { return id; }
    public int roomNumber() { return roomNumber; }
    public Guest guest() { return guest; }
    public LocalDate arrival() { return checkIn; }
    public LocalDate departure() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
