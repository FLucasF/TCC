package br.com.mare.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

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
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, GuestIdentity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identity = identity;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(Room room, String guestName, String guestEmail, GuestIdentity identity,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Quote quote = PricingPolicy.quote(room, ratePlan, checkIn, checkOut);
        return new Reservation(0, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public Reservation withId(int newId) {
        return new Reservation(newId, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                quote, status, amountPaid, refund);
    }

    public void confirm() {
        move(ReservationStatus.CONFIRMED, ReservationStatus.PENDING);
        amountPaid = quote.total();
    }

    public void startStay() {
        move(ReservationStatus.CHECKED_IN, ReservationStatus.CONFIRMED);
    }

    public void endStay() {
        move(ReservationStatus.CHECKED_OUT, ReservationStatus.CHECKED_IN);
    }

    public void cancel() {
        move(ReservationStatus.CANCELLED, ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundOf(amountPaid);
    }

    private void move(ReservationStatus target, ReservationStatus... allowedFrom) {
        if (!Arrays.asList(allowedFrom).contains(status)) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva com status " + status);
        }
        status = target;
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
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
