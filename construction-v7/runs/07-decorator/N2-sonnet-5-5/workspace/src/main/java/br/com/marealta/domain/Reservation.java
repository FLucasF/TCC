package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.marealta.domain.ReservationStatus.*;

public class Reservation {
    private final int id;
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

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
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
        this.quote = Quote.of(room, checkIn, checkOut, ratePlan);
    }

    public void confirm() {
        require(PENDING, "Só é possível confirmar uma reserva pendente");
        status = CONFIRMED;
        amountPaid = quote.total();
    }

    public void performCheckIn() {
        require(CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = CHECKED_IN;
    }

    public void performCheckOut() {
        require(CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
        status = CHECKED_OUT;
    }

    public void cancel() {
        if (status != PENDING && status != CONFIRMED) {
            throw DomainException.notAllowed("Só é possível cancelar antes do check-in");
        }
        refund = ratePlan.refund(amountPaid);
        status = CANCELLED;
    }

    private void require(ReservationStatus expected, String message) {
        if (status != expected) throw DomainException.notAllowed(message);
    }

    public boolean overlaps(LocalDate in, LocalDate out) {
        return status != CANCELLED && checkIn.isBefore(out) && in.isBefore(checkOut);
    }

    public boolean occupiesNight(LocalDate night) {
        return status.isPaid() && !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public int id() { return id; }
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
