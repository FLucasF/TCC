package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {
    private Long id;
    private final int roomNumber;
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

    public Reservation(Long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
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

    public static Reservation pending(ReservationRequest request, Quote quote) {
        return new Reservation(null, request.roomNumber(), request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED, "Só é possível confirmar uma reserva pendente");
        amountPaid = quote.total();
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED, "Só é possível cancelar antes do check-in");
        refund = ratePlan.refund(amountPaid);
    }

    public void startStay() {
        moveTo(ReservationStatus.CHECKED_IN, "Só é possível fazer check-in de uma reserva confirmada");
    }

    public void endStay() {
        moveTo(ReservationStatus.CHECKED_OUT, "Só é possível fazer check-out de quem fez check-in");
    }

    private void moveTo(ReservationStatus target, String refusal) {
        if (!status.canMoveTo(target)) {
            throw ApiException.operationNotAllowed(refusal);
        }
        status = target;
    }

    /** Dinheiro que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void assignId(long id) { this.id = id; }

    public Long id() { return id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public int nights() { return quote.nights(); }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
