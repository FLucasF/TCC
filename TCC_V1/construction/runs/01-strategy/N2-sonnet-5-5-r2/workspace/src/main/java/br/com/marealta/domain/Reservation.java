package br.com.marealta.domain;

import br.com.marealta.plan.RatePlan;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
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
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(Room room, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                      Quote quote) {
        return new Reservation(0, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = total();
    }

    public void checkIn() {
        requireStatus("com check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        requireStatus("com check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        status = ReservationStatus.CANCELLED;
        refund = ratePlan.refund(amountPaid);
    }

    /** Comprovante, que só existe para reserva paga. */
    public Receipt receipt() {
        if (!status.isPaid()) {
            throw BusinessException.notAllowed("Reserva " + id + " não está paga: " + status);
        }
        return new Receipt(id, guestName, guestCpf, room.number(), room.type(), checkIn, checkOut,
                nights(), subtotal(), discount(), total());
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw BusinessException.notAllowed("Reserva " + id + " não pode ser " + action + " na situação " + status);
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return quote.nights(); }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    /** O que ficou com a pousada: o pago menos o devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }
}
