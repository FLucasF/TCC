package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

public class Reservation {
    private Long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentity identity;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Price price;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, Room room, String guestName, String guestEmail, GuestIdentity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Price price,
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
        this.price = price;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(Room room, String guestName, String guestEmail, GuestIdentity identity,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                      Price price) {
        return new Reservation(null, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                price, ReservationStatus.PENDING, Money.zero(), null);
    }

    public void confirm() {
        require("confirmar", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = price.total();
    }

    public void startStay() {
        require("fazer check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        require("fazer check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundable() ? amountPaid : Money.zero();
        status = ReservationStatus.CANCELLED;
    }

    private void require(String operation, ReservationStatus... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw AppException.notAllowed("Não é possível " + operation + " uma reserva com situação " + status + ".");
        }
    }

    /** O que fica com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    /** Ocupa o quarto na noite informada (entrada inclusive, saída exclusive). */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public void assignId(long id) { this.id = id; }

    public Long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public GuestIdentity identity() { return identity; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Price price() { return price; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
