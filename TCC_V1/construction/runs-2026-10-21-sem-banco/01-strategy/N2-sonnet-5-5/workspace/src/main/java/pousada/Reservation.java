package pousada;

import pousada.plan.RatePlan;

import java.math.BigDecimal;
import java.time.LocalDate;

import static pousada.ReservationStatus.*;

public class Reservation {
    public final int id;
    public final Room room;
    public final String guestName, guestEmail, guestCpf;
    public final LocalDate checkIn, checkOut;
    public final int guests;
    public final RatePlan plan;
    public final Quote quote;
    private ReservationStatus status = PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan, Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.plan = plan;
        this.quote = quote;
    }

    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    public BigDecimal netAmount() {
        return Money.round(amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund));
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public void confirm() {
        require("confirmar", PENDING);
        status = CONFIRMED;
        amountPaid = quote.total();
    }

    public void checkIn() {
        require("fazer check-in de", CONFIRMED);
        status = CHECKED_IN;
    }

    public void checkOut() {
        require("fazer check-out de", CHECKED_IN);
        status = CHECKED_OUT;
    }

    public void cancel() {
        require("cancelar", PENDING, CONFIRMED);
        refund = plan.refund(amountPaid);
        status = CANCELLED;
    }

    private void require(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) return;
        }
        throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível " + action + " uma reserva com situação " + status);
    }
}
