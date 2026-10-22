package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

record Reservation(long id, Room room, String guestName, String guestEmail, Identification identification,
                   LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Pricing pricing,
                   Status status, BigDecimal amountPaid, BigDecimal refund) {

    Reservation confirm() {
        requireStatus("confirmada", Status.PENDING);
        return withStatus(Status.CONFIRMED, pricing.total(), refund);
    }

    Reservation doCheckIn() {
        requireStatus("com check-in", Status.CONFIRMED);
        return withStatus(Status.CHECKED_IN, amountPaid, refund);
    }

    Reservation doCheckOut() {
        requireStatus("com check-out", Status.CHECKED_IN);
        return withStatus(Status.CHECKED_OUT, amountPaid, refund);
    }

    Reservation cancel() {
        requireStatus("cancelada", Status.PENDING, Status.CONFIRMED);
        return withStatus(Status.CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    BigDecimal retained() {
        return amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund);
    }

    private void requireStatus(String action, Status... allowed) {
        for (Status s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw ApiException.notAllowed("A reserva está " + status + " e não pode ficar " + action + ".");
    }

    private Reservation withStatus(Status newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, room, guestName, guestEmail, identification, checkIn, checkOut, guests,
                ratePlan, pricing, newStatus, paid, newRefund);
    }
}
