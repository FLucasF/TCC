package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public class Reservation {

    private final Long id;
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

    public Reservation(Long id, Room room, String guestName, String guestEmail, String guestCpf,
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

    /** Nova reserva pendente; valida a capacidade do quarto. */
    public static Reservation open(Room room, String guestName, String guestEmail, String guestCpf,
                                   LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (guests > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Quote quote = Quote.of(room, checkIn, checkOut, ratePlan);
        return new Reservation(null, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, status, amountPaid, refund);
    }

    public void confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        amountPaid = quote.total();
        status = ReservationStatus.CONFIRMED;
    }

    public void registerCheckIn() {
        requireStatus("feito check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void registerCheckOut() {
        requireStatus("feito check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    /** Só reservas pagas têm comprovante. */
    public Receipt receipt() {
        if (!status.isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só reservas pagas têm comprovante; situação atual: " + status);
        }
        return Receipt.of(this);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        if (!Set.of(allowed).contains(status)) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva não pode ser " + action + " na situação " + status);
        }
    }

    public Long id() { return id; }
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
