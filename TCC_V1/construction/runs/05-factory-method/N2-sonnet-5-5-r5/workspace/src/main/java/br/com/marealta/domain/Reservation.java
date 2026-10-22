package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentity identity;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, GuestIdentity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
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
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    /** Nova reserva pendente, com os valores já calculados. */
    public static Reservation book(Room room, String guestName, String guestEmail, GuestIdentity identity,
                                   LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Pricing.Quote quote = Pricing.quote(room.type(), checkIn, checkOut, ratePlan);
        return new Reservation(0, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED);
        amountPaid = total;
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED);
        refund = ratePlan.refundOf(amountPaid);
    }

    public void registerCheckIn() {
        moveTo(ReservationStatus.CHECKED_IN);
    }

    public void registerCheckOut() {
        moveTo(ReservationStatus.CHECKED_OUT);
    }

    private void moveTo(ReservationStatus next) {
        if (!status.canMoveTo(next)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva " + status);
        }
        status = next;
    }

    public ReservationDocument document() {
        if (!status.isPaid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só existe documento para reserva paga; situação atual: " + status);
        }
        return identity.documentFor(this);
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    /** A reserva ocupa o quarto nesta noite (entrada conta, saída não). */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void assignId(long id) {
        this.id = id;
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public GuestIdentity identity() { return identity; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
