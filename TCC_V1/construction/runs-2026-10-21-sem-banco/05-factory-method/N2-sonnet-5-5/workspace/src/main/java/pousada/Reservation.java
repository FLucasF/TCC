package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final int id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final GuestIdentity identity;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Quote quote;
    private Status status = Status.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, GuestIdentity identity,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identity = identity;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = Quote.of(room, checkIn, checkOut, ratePlan);
    }

    public synchronized void confirm() {
        require("confirmar", Status.PENDING);
        amountPaid = quote.total();
        status = Status.CONFIRMED;
    }

    public synchronized void checkIn() {
        require("fazer check-in", Status.CONFIRMED);
        status = Status.CHECKED_IN;
    }

    public synchronized void checkOut() {
        require("fazer check-out", Status.CHECKED_IN);
        status = Status.CHECKED_OUT;
    }

    public synchronized void cancel() {
        require("cancelar", Status.PENDING, Status.CONFIRMED);
        refund = ratePlan.refundFor(amountPaid);
        status = Status.CANCELLED;
    }

    private void require(String operation, Status... allowed) {
        for (Status s : allowed) {
            if (status == s) return;
        }
        throw ApiException.operationNotAllowed(
                "Não é possível " + operation + " uma reserva com status " + status);
    }

    /** Documento da reserva; só existe para reserva paga. */
    public synchronized ReservationDocument document() {
        if (!status.isPaid()) {
            throw ApiException.operationNotAllowed(
                    "Só existe documento para reserva paga; status atual: " + status);
        }
        return identity.issue(new DocumentData(id, guestName, room.number(), room.type(), checkIn, checkOut, quote));
    }

    public synchronized boolean occupies(LocalDate night) {
        return status.isPaid() && !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate in, LocalDate out) {
        return getStatus().holdsRoom() && in.isBefore(checkOut) && checkIn.isBefore(out);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public synchronized BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public GuestIdentity identity() { return identity; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public synchronized Status getStatus() { return status; }
    public synchronized BigDecimal amountPaid() { return amountPaid; }
    public synchronized BigDecimal refund() { return refund; }
}
