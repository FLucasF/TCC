package br.com.marealta.domain;

import br.com.marealta.identity.GuestIdentity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public class Reservation {
    private final long id;
    private final int roomNumber;
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

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
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

    public void confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = quote.total();
    }

    public void startStay() {
        requireStatus("feito check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        requireStatus("feito check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.isRefundable() ? amountPaid : Money.ZERO;
        status = ReservationStatus.CANCELLED;
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw AppException.notAllowed("A reserva " + id + " não está paga (situação " + status + ")");
        }
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal netAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        if (!Set.of(allowed).contains(status)) {
            throw AppException.notAllowed("A reserva " + id + " não pode ser " + action + " na situação " + status);
        }
    }

    public long id() { return id; }
    public int roomNumber() { return roomNumber; }
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
