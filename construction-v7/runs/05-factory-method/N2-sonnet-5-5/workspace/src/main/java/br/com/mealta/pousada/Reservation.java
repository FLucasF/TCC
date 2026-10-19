package br.com.mealta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final Identity identity;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Pricing.Quote quote;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.round(BigDecimal.ZERO);
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, Identity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Pricing.Quote quote) {
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
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public Identity identity() { return identity; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Pricing.Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund));
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public boolean occupiesNight(LocalDate night) {
        return status.isPaid() && !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        amountPaid = quote.total();
        status = ReservationStatus.CONFIRMED;
    }

    public void startStay() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void finishStay() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public DocumentData documentData() {
        return new DocumentData(id, guestName, room.number(), room.type(), checkIn, checkOut,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total());
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw DomainException.notAllowed(message);
    }
}
