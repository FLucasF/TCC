package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

import pousada.identity.GuestIdentity;

public class Reservation {
    private final int id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentity identity;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, GuestIdentity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identity = identity;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = Quote.of(room, checkIn, checkOut, ratePlan);
    }

    public void confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        amountPaid = quote.total();
        status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundOf(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public void startStay() {
        requireStatus("liberada para check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        requireStatus("liberada para check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw ApiException.notAllowed("A reserva " + id + " está " + status + " e não pode ser " + action + ".");
    }

    /** Valor que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public GuestIdentity identity() { return identity; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
