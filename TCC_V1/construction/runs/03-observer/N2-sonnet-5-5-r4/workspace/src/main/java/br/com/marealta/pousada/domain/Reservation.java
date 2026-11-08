package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.marealta.pousada.domain.ReservationStatus.*;

public class Reservation {

    private final long id;
    private final Room room;
    private final Guest guest;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, Room room, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests,
                       RatePlan ratePlan, Quote quote, ReservationStatus status,
                       BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.room = room;
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

    public static Reservation pending(long id, Room room, Guest guest, LocalDate checkIn, LocalDate checkOut,
                                      int guests, RatePlan ratePlan) {
        Quote quote = Quote.of(room, checkIn, checkOut, ratePlan);
        return new Reservation(id, room, guest, checkIn, checkOut, guests, ratePlan, quote, PENDING,
                Money.ZERO, null);
    }

    /** Guarda a mesma reserva com outro número (o que o banco atribuiu). */
    public Reservation withId(long newId) {
        return new Reservation(newId, room, guest, checkIn, checkOut, guests, ratePlan, quote, status,
                amountPaid, refund);
    }

    public void confirm() {
        require(status == PENDING, "só é possível confirmar uma reserva pendente");
        status = CONFIRMED;
        amountPaid = quote.total();
    }

    public void startStay() {
        require(status == CONFIRMED, "só é possível fazer check-in de uma reserva confirmada");
        status = CHECKED_IN;
    }

    public void endStay() {
        require(status == CHECKED_IN, "só é possível fazer check-out de uma reserva com check-in");
        status = CHECKED_OUT;
    }

    public void cancel() {
        require(status == PENDING || status == CONFIRMED,
                "só é possível cancelar uma reserva pendente ou confirmada");
        refund = status == CONFIRMED ? ratePlan.refundFor(amountPaid) : Money.ZERO;
        status = CANCELLED;
    }

    private void require(boolean allowed, String message) {
        if (!allowed) {
            throw new DomainException(DomainException.Kind.OPERATION_NOT_ALLOWED, message);
        }
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public long id() { return id; }
    public Room room() { return room; }
    public Guest guest() { return guest; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
