package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static pousada.domain.ReservationStatus.*;

/** A reserva, no formato em que o site a recebe. Cada transição devolve a reserva atualizada. */
public record Reservation(
        long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
        BigDecimal subtotal, BigDecimal discount, BigDecimal total,
        ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    public Reservation confirm() {
        requireStatus("confirmar", PENDING);
        return with(CONFIRMED, total, refund);
    }

    public Reservation checkInGuest() {
        requireStatus("fazer check-in de", CONFIRMED);
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkOutGuest() {
        requireStatus("fazer check-out de", CHECKED_IN);
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel(RatePlan plan) {
        requireStatus("cancelar", PENDING, CONFIRMED);
        return with(CANCELLED, amountPaid, plan.refund(amountPaid));
    }

    public Reservation requirePaid() {
        if (!status.isPaid()) {
            throw ApiException.operationNotAllowed("A reserva não está paga, não há comprovante");
        }
        return this;
    }

    public BigDecimal amountKept() {
        return Money.round(amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund));
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) return;
        }
        throw ApiException.operationNotAllowed("Não é possível " + action + " uma reserva " + status);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, paid, newRefund);
    }
}
