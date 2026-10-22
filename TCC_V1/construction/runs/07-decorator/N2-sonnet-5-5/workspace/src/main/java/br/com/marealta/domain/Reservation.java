package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private Long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Pricing.Quote quote;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Pricing.Quote quote, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
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

    public static Reservation pending(Room room, NewReservation request) {
        Pricing.Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
        return new Reservation(null, room.number(), request.guestName(), request.guestEmail(), request.guestCpf(),
                request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(), quote,
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        require("confirmar", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = quote.total();
    }

    public void registerCheckIn() {
        require("fazer check-in de", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void registerCheckOut() {
        require("fazer check-out de", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        status = ReservationStatus.CANCELLED;
        refund = ratePlan.refundOf(amountPaid);
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw ApiException.operationNotAllowed("Só existe comprovante de reserva paga");
        }
    }

    private void require(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) return;
        }
        throw ApiException.operationNotAllowed("Não é possível " + action + " uma reserva " + status);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal amountKept() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    public Long id() { return id; }
    public void assignId(long id) { this.id = id; }
    public int roomNumber() { return roomNumber; }
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
}
