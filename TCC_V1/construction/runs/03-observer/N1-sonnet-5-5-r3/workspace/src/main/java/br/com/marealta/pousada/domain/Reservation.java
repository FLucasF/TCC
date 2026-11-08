package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id,
        int roomNumber,
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

    public Reservation confirm() {
        return move(ReservationStatus.CONFIRMED, total, null);
    }

    public Reservation checkInDone() {
        return move(ReservationStatus.CHECKED_IN, amountPaid, null);
    }

    public Reservation checkOutDone() {
        return move(ReservationStatus.CHECKED_OUT, amountPaid, null);
    }

    public Reservation cancel() {
        return move(ReservationStatus.CANCELLED, amountPaid, ratePlan.refund(amountPaid));
    }

    private Reservation move(ReservationStatus next, BigDecimal paid, BigDecimal refund) {
        if (!status.canMoveTo(next)) {
            throw new NotAllowedException("Reserva " + id + " está " + status + " e não pode ir para " + next);
        }
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, next, paid, refund);
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }
}
