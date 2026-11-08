package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public static Reservation pending(Room room, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Pricing p = Pricing.quote(room, checkIn, checkOut, ratePlan);
        return new Reservation(null, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, p.nights(), p.subtotal(), p.discount(), p.total(), ReservationStatus.PENDING,
                Money.ZERO, null);
    }

    public Reservation confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        return copy(ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        requireStatus("feito o check-in", ReservationStatus.CONFIRMED);
        return copy(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        requireStatus("feito o check-out", ReservationStatus.CHECKED_IN);
        return copy(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        BigDecimal toRefund = ratePlan == RatePlan.STANDARD ? amountPaid : Money.ZERO;
        return copy(ReservationStatus.CANCELLED, amountPaid, toRefund);
    }

    /** Reserva paga (confirmada, com check-in ou com check-out). */
    public boolean paid() {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT;
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    private Reservation copy(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) {
                return;
            }
        }
        throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                "A reserva " + id + " está " + status + " e não pode ser " + action);
    }
}
