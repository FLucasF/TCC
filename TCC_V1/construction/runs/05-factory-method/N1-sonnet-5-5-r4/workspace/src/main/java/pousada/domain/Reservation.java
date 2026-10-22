package pousada.domain;

import pousada.document.Stay;
import pousada.identity.Identity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        Quote quote,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public static Reservation pending(int roomNumber, String guestName, String guestEmail, Identity identity,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                      Quote quote) {
        return new Reservation(0, roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests,
                ratePlan, quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public Reservation confirm() {
        return moveTo(ReservationStatus.CONFIRMED, quote.total(), refund);
    }

    public Reservation registerCheckIn() {
        return moveTo(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation registerCheckOut() {
        return moveTo(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        return moveTo(ReservationStatus.CANCELLED, amountPaid, ratePlan.refundOf(amountPaid));
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests,
                ratePlan, quote, status, amountPaid, refund);
    }

    public Stay stay(Room room) {
        return new Stay(id, guestName, room.number(), room.type(), checkIn, checkOut,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total());
    }

    private Reservation moveTo(ReservationStatus target, BigDecimal newAmountPaid, BigDecimal newRefund) {
        if (!status.next().contains(target)) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserva " + status + " não pode passar para " + target);
        }
        return new Reservation(id, roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests,
                ratePlan, quote, target, newAmountPaid, newRefund);
    }
}
