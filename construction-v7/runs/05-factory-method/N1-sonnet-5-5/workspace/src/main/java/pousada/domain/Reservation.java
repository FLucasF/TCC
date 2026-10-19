package pousada.domain;

import pousada.domain.Pricing.Quote;
import pousada.domain.document.Document;
import pousada.domain.document.Voucher;
import pousada.domain.identification.Identification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {

    private final long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final Identification identification;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;

    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.round(BigDecimal.ZERO);
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, Identification identification,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        status = ReservationStatus.CONFIRMED;
        amountPaid = quote.total();
    }

    public void startStay() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        refund = ratePlan.refundable() ? amountPaid : Money.round(BigDecimal.ZERO);
        status = ReservationStatus.CANCELLED;
    }

    /** Ocupa o quarto nas noites de entrada (inclusive) até saída (exclusive), se não cancelada. */
    public boolean occupies(int roomNumber, LocalDate from, LocalDate to) {
        return status != ReservationStatus.CANCELLED && room.number() == roomNumber
                && checkIn.isBefore(to) && from.isBefore(checkOut);
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    /** Documento da reserva paga; a segunda via só existe para reserva paga. */
    public Document document() {
        require(status.isPaid(), "O documento só existe para reserva paga");
        Voucher voucher = new Voucher(id, guestName, room.number(), room.type(), checkIn, checkOut,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total());
        return identification.issue(voucher);
    }

    private static void require(boolean allowed, String message) {
        if (!allowed) {
            throw DomainException.notAllowed(message);
        }
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public Identification identification() { return identification; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
