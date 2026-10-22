package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

public class Reservation {
    private long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final Identity identity;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, Identity identity,
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

    public static Reservation create(Room room, String guestName, String guestEmail, Identity identity,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Quote quote = Quote.of(room.type(), checkIn, checkOut, ratePlan);
        return new Reservation(0, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                quote, ReservationStatus.PENDING, Money.round(BigDecimal.ZERO), null);
    }

    public void confirm() {
        transition(ReservationStatus.CONFIRMED, ReservationStatus.PENDING);
        amountPaid = quote.total();
    }

    public void checkIn() {
        transition(ReservationStatus.CHECKED_IN, ReservationStatus.CONFIRMED);
    }

    public void checkOut() {
        transition(ReservationStatus.CHECKED_OUT, ReservationStatus.CHECKED_IN);
    }

    public void cancel() {
        transition(ReservationStatus.CANCELLED, ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refund(amountPaid);
    }

    private void transition(ReservationStatus to, ReservationStatus... allowedFrom) {
        if (!Arrays.asList(allowedFrom).contains(status)) {
            throw new OperationNotAllowedException(
                    "Operação não permitida para reserva com situação " + status);
        }
        status = to;
    }

    public ReceiptData receiptData() {
        return new ReceiptData(id, guestName, room.number(), room.type(), checkIn, checkOut,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total());
    }

    /** Segunda via e documento do check-out só existem para reserva paga. */
    public Document document() {
        if (!status.isPaid()) {
            throw new OperationNotAllowedException("Só reservas pagas têm documento");
        }
        return identity.document(receiptData());
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public void assignId(long id) {
        this.id = id;
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public Identity identity() { return identity; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
