package br.com.marealta.reservation;

import br.com.marealta.Money;
import br.com.marealta.web.ApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public record Reservation(Long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                          LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                          BigDecimal subtotal, BigDecimal discount, BigDecimal total, Status status,
                          BigDecimal amountPaid, BigDecimal refund) {

    public Reservation confirm() {
        requireStatus("confirmar", EnumSet.of(Status.PENDING));
        return with(Status.CONFIRMED, total, refund);
    }

    public Reservation checkInGuest() {
        requireStatus("fazer o check-in", EnumSet.of(Status.CONFIRMED));
        return with(Status.CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkOutGuest() {
        requireStatus("fazer o check-out", EnumSet.of(Status.CHECKED_IN));
        return with(Status.CHECKED_OUT, amountPaid, refund);
    }

    /** Pendente não pagou nada, então não tem o que devolver; o resto segue o plano de tarifa. */
    public Reservation cancel() {
        requireStatus("cancelar", EnumSet.of(Status.PENDING, Status.CONFIRMED));
        return with(Status.CANCELLED, amountPaid, ratePlan.refundOn(amountPaid));
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal kept() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    private void requireStatus(String operation, Set<Status> allowed) {
        if (!allowed.contains(status)) {
            throw ApiException.operationNotAllowed(
                    "Não é possível " + operation + " uma reserva com status " + status);
        }
    }

    private Reservation with(Status newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
