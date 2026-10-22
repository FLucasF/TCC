package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import pousada.ApiException;

import static pousada.domain.ReservationStatus.*;

public record Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        Identity identity,
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

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public Reservation confirm() {
        return transition(Set.of(PENDING), CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        return transition(Set.of(CONFIRMED), CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        return transition(Set.of(CHECKED_IN), CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        return transition(Set.of(PENDING, CONFIRMED), CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    public boolean isPaid() {
        return PAID.contains(status);
    }

    /** O que a pousada guarda: o que foi pago menos o que foi devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    private Reservation transition(Set<ReservationStatus> allowedFrom, ReservationStatus to,
                                   BigDecimal paid, BigDecimal newRefund) {
        if (!allowedFrom.contains(status)) {
            throw ApiException.notAllowed("Reserva " + id + " está " + status + " e não pode passar para " + to);
        }
        return new Reservation(id, roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, to, paid, newRefund);
    }
}
