package br.com.maralta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        Identification identification,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        Status status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public Reservation confirm() {
        requireStatus("confirmada", Status.PENDING);
        return copy(Status.CONFIRMED, total, refund);
    }

    public Reservation checkInGuest() {
        requireStatus("com check-in", Status.CONFIRMED);
        return copy(Status.CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkOutGuest() {
        requireStatus("com check-out", Status.CHECKED_IN);
        return copy(Status.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", Status.PENDING, Status.CONFIRMED);
        return copy(Status.CANCELLED, amountPaid, ratePlan.refundOf(amountPaid));
    }

    /** Dinheiro que fica com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw ApiException.operationNotAllowed("Reserva " + id + " não está paga (" + status + ")");
        }
    }

    private void requireStatus(String action, Status... allowed) {
        for (Status s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw ApiException.operationNotAllowed("Reserva " + id + " não pode ser " + action + " estando " + status);
    }

    private Reservation copy(Status newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, identification, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
