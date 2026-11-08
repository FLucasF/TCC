package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import pousada.error.ApiException;

import static pousada.domain.ReservationStatus.CANCELLED;
import static pousada.domain.ReservationStatus.CHECKED_IN;
import static pousada.domain.ReservationStatus.CHECKED_OUT;
import static pousada.domain.ReservationStatus.CONFIRMED;
import static pousada.domain.ReservationStatus.PENDING;

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

    public Reservation afterConfirmation() {
        return moveTo(CONFIRMED, total, refund, PENDING);
    }

    public Reservation afterCheckIn() {
        return moveTo(CHECKED_IN, amountPaid, refund, CONFIRMED);
    }

    public Reservation afterCheckOut() {
        return moveTo(CHECKED_OUT, amountPaid, refund, CHECKED_IN);
    }

    public Reservation afterCancellation() {
        return moveTo(CANCELLED, amountPaid, ratePlan.refundFor(amountPaid), PENDING, CONFIRMED);
    }

    /** Segunda via e comprovante só existem para reserva paga. */
    public void requirePaid() {
        if (!status.paid()) {
            throw ApiException.operationNotAllowed("A reserva não está paga");
        }
    }

    /** Dinheiro que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    private Reservation moveTo(ReservationStatus next, BigDecimal paid, BigDecimal refunded, ReservationStatus... allowedFrom) {
        if (!Set.of(allowedFrom).contains(status)) {
            throw ApiException.operationNotAllowed("Operação não permitida para reserva " + status);
        }
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, next, paid, refunded);
    }
}
