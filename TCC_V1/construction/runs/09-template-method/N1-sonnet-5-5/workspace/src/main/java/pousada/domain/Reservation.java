package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import pousada.error.BusinessException;
import pousada.error.ErrorCode;

public record Reservation(
        int id,
        Room room,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        Quote quote,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public int nights() {
        return quote.nights();
    }

    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOut);
    }

    public boolean occupiesNight(LocalDate night) {
        return !checkIn.isAfter(night) && night.isBefore(checkOut);
    }

    public BigDecimal heldAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public Reservation confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        return next(ReservationStatus.CONFIRMED, quote.total(), refund);
    }

    public Reservation doCheckIn() {
        requireStatus("com check-in", ReservationStatus.CONFIRMED);
        return next(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation doCheckOut() {
        requireStatus("com check-out", ReservationStatus.CHECKED_IN);
        return next(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        return next(ReservationStatus.CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw notAllowed("A reserva não está paga");
        }
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) {
                return;
            }
        }
        throw notAllowed("A reserva " + id + " não pode ser " + action + " com a situação " + status);
    }

    private static BusinessException notAllowed(String message) {
        return new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }

    private Reservation next(ReservationStatus newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, newStatus, paid, newRefund);
    }
}
