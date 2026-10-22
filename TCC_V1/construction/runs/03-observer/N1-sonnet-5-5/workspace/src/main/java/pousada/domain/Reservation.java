package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static pousada.domain.ReservationStatus.*;

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

    /** Reserva nova, pendente, ainda sem número. */
    public static Reservation open(Room room, Guest guest, LocalDate checkIn, LocalDate checkOut,
                                   int guests, RatePlan ratePlan) {
        if (guests > room.capacity()) {
            throw new DomainException(DomainException.Code.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        return new Reservation(0, room, guest, checkIn, checkOut, guests, ratePlan,
                Quote.of(room, checkIn, checkOut, ratePlan), PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, room, guest, checkIn, checkOut, guests, ratePlan, quote,
                status, amountPaid, refund);
    }

    public void confirm() {
        moveTo(CONFIRMED, PENDING);
        amountPaid = quote.total();
    }

    public void startStay() {
        moveTo(CHECKED_IN, CONFIRMED);
    }

    public void endStay() {
        moveTo(CHECKED_OUT, CHECKED_IN);
    }

    public void cancel() {
        moveTo(CANCELLED, PENDING, CONFIRMED);
        refund = ratePlan.refundOf(amountPaid);
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    private void moveTo(ReservationStatus target, ReservationStatus... allowedFrom) {
        if (!Arrays.asList(allowedFrom).contains(status)) {
            throw new DomainException(DomainException.Code.OPERATION_NOT_ALLOWED,
                    "Reserva " + id + " está " + status + " e não pode ir para " + target);
        }
        status = target;
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
