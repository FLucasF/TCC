package br.com.pousadamarealta.reservas.domain;

import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CANCELLED;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CHECKED_IN;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CHECKED_OUT;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CONFIRMED;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.PENDING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

public final class Reservation {

    private final long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;
    private ReservationStatus status = PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = Pricing.quote(room, checkIn, checkOut, ratePlan);
    }

    public void confirm() {
        moveTo(CONFIRMED, PENDING);
        amountPaid = quote.total();
    }

    public void registerCheckIn() {
        moveTo(CHECKED_IN, CONFIRMED);
    }

    public void registerCheckOut() {
        moveTo(CHECKED_OUT, CHECKED_IN);
    }

    public void cancel() {
        moveTo(CANCELLED, PENDING, CONFIRMED);
        refund = ratePlan.refund(amountPaid);
    }

    /** Só reservas pagas têm comprovante. */
    public void requirePaid() {
        if (!status.isPaid()) {
            throw notAllowed("só existe comprovante para reserva paga");
        }
    }

    /** A reserva ocupa a noite que começa na data informada (a data de saída não conta). */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOut);
    }

    public boolean isActive() {
        return status != CANCELLED;
    }

    /** O dinheiro que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    private void moveTo(ReservationStatus target, ReservationStatus... allowedFrom) {
        if (!Arrays.asList(allowedFrom).contains(status)) {
            throw notAllowed("reserva " + status + " não pode ir para " + target);
        }
        status = target;
    }

    private DomainException notAllowed(String message) {
        return new DomainException(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
