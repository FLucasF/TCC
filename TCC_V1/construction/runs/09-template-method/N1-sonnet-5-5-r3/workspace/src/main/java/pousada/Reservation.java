package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

import static pousada.ReservationStatus.*;

public record Reservation(
        Long id,
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
        requireStatus("confirmar", PENDING);
        return with(CONFIRMED, total, refund);
    }

    public Reservation registerCheckIn() {
        requireStatus("fazer check-in de", CONFIRMED);
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation registerCheckOut() {
        requireStatus("fazer check-out de", CHECKED_IN);
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelar", PENDING, CONFIRMED);
        return with(CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) {
                return;
            }
        }
        throw ApiException.operationNotAllowed("Não é possível " + action + " uma reserva com situação " + status);
    }
}
