package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public Reservation(Long id, Room room, String guestName, String guestEmail, String guestCpf, LocalDate checkIn,
                       LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote, ReservationStatus status,
                       BigDecimal amountPaid, BigDecimal refund) {
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

    public static Reservation open(NewReservation request, Room room) {
        if (request.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas");
        }
        Quote quote = Quote.of(room.type(), request.checkIn(), request.checkOut(), request.ratePlan());
        return new Reservation(null, room, request.guestName(), request.guestEmail(), request.guestCpf(),
                request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(), quote,
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                quote, status, amountPaid, refund);
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED, "confirmada");
        amountPaid = quote.total();
    }

    public void startStay() {
        moveTo(ReservationStatus.CHECKED_IN, "marcada com check-in");
    }

    public void endStay() {
        moveTo(ReservationStatus.CHECKED_OUT, "marcada com check-out");
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED, "cancelada");
        refund = ratePlan.refundOf(amountPaid);
    }

    public Receipt receipt() {
        if (!status.isPaid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só existe comprovante para reserva paga (situação atual: " + status + ")");
        }
        return Receipt.of(this);
    }

    private void moveTo(ReservationStatus next, String action) {
        if (!status.canMoveTo(next)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva não pode ser " + action + " na situação " + status);
        }
        status = next;
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
    public int nights() { return quote.nights(); }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }
}
