package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

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
        Status status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public Reservation confirm() {
        requireStatus("confirmada", Status.PENDING);
        return with(Status.CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        requireStatus("feito o check-in", Status.CONFIRMED);
        return with(Status.CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        requireStatus("feito o check-out", Status.CHECKED_IN);
        return with(Status.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", Status.PENDING, Status.CONFIRMED);
        return with(Status.CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    private void requireStatus(String action, Status... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva " + id + " está " + status + " e não pode ser " + action);
        }
    }

    private Reservation with(Status newStatus, BigDecimal newAmountPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newAmountPaid, newRefund);
    }
}
