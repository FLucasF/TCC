package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Immutable snapshot of a reservation; state changes produce a new instance. */
public record Reservation(
        long id,
        Room room,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(id, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, newStatus, amountPaid, refund);
    }

    public Reservation confirmed() {
        return new Reservation(id, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation cancelled(BigDecimal refundAmount) {
        return new Reservation(id, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, ReservationStatus.CANCELLED, amountPaid, refundAmount);
    }

    /** Whether this reservation occupies the room on the given night (check-out day is free). */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    /** Money kept by the inn: paid minus refunded. */
    public BigDecimal netRevenue() {
        return Money.round(refund == null ? amountPaid : amountPaid.subtract(refund));
    }
}
