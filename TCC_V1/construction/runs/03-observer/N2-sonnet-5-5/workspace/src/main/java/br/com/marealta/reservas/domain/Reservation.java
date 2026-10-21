package br.com.marealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final int id;
    private final Room room;
    private final NewReservation request;
    private final Quote quote;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, NewReservation request) {
        this.id = id;
        this.room = room;
        this.request = request;
        this.quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
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
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out de quem fez check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar uma reserva pendente ou confirmada");
        status = ReservationStatus.CANCELLED;
        refund = request.ratePlan().refundOf(amountPaid);
    }

    private void require(boolean allowed, String message) {
        if (!allowed) throw DomainException.operationNotAllowed(message);
    }

    public boolean overlaps(Room other, LocalDate in, LocalDate out) {
        return status != ReservationStatus.CANCELLED && room.equals(other)
                && checkIn().isBefore(out) && in.isBefore(checkOut());
    }

    public boolean occupiesNight(LocalDate night) {
        return status.isPaid() && !night.isBefore(checkIn()) && night.isBefore(checkOut());
    }

    /** What stayed with the inn: paid minus refunded. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return request.guestName(); }
    public String guestEmail() { return request.guestEmail(); }
    public String guestCpf() { return request.guestCpf(); }
    public LocalDate checkIn() { return request.checkIn(); }
    public LocalDate checkOut() { return request.checkOut(); }
    public int guests() { return request.guests(); }
    public RatePlan ratePlan() { return request.ratePlan(); }
    public int nights() { return quote.nights(); }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
