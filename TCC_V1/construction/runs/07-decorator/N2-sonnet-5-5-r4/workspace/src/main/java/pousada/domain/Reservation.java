package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static pousada.domain.ReservationStatus.*;

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

    public Reservation confirmed() {
        requireStatus("confirmar", PENDING);
        return with(CONFIRMED, total, refund);
    }

    public Reservation checkedIn() {
        requireStatus("fazer check-in de", CONFIRMED);
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkedOut() {
        requireStatus("fazer check-out de", CHECKED_IN);
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancelled() {
        requireStatus("cancelar", PENDING, CONFIRMED);
        return with(CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean paid() {
        return status == CONFIRMED || status == CHECKED_IN || status == CHECKED_OUT;
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public void requirePaid() {
        if (!paid()) {
            throw ApiException.operationNotAllowed("A reserva " + id + " não está paga");
        }
    }

    private void requireStatus(String operation, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw ApiException.operationNotAllowed(
                "Não é possível " + operation + " uma reserva com situação " + status);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
