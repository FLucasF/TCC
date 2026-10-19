package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final int id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = Pricing.subtotal(room, checkIn, checkOut);
        this.discount = ratePlan.discount(subtotal);
        this.total = Money.round(subtotal.subtract(discount));
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void registerCheckIn() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void registerCheckOut() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar uma reserva pendente ou confirmada");
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw ApiException.notAllowed(message);
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
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
