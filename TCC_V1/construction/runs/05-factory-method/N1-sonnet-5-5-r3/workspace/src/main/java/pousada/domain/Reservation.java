package pousada.domain;

import pousada.BusinessException;
import pousada.ErrorCode;
import pousada.identity.GuestIdentity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static pousada.domain.ReservationStatus.*;

/** refund é null enquanto a reserva não for cancelada. */
public record Reservation(
        long id,
        Room room,
        String guestName,
        String guestEmail,
        GuestIdentity identity,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        Quote quote,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public static Reservation pending(Room room, NewReservation n) {
        Quote quote = Quote.of(room, n.checkIn(), n.checkOut(), n.ratePlan());
        return new Reservation(0, room, n.guestName(), n.guestEmail(), n.identity(), n.checkIn(), n.checkOut(),
                n.guests(), n.ratePlan(), quote, PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                quote, status, amountPaid, refund);
    }

    public Reservation confirm() {
        requireStatus("confirmada", PENDING);
        return with(CONFIRMED, quote.total(), refund);
    }

    public Reservation doCheckIn() {
        requireStatus("com check-in", CONFIRMED);
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation doCheckOut() {
        requireStatus("com check-out", CHECKED_IN);
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", PENDING, CONFIRMED);
        return with(CANCELLED, amountPaid, ratePlan.refundOf(amountPaid));
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só existe segunda via de reserva paga; a reserva está " + status);
        }
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, room, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan,
                quote, newStatus, paid, newRefund);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva está " + status + " e não pode ser " + action);
        }
    }
}
