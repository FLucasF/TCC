package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.marealta.domain.ReservationStatus.*;

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

    /** Confirmed, checked in or checked out: the guest has paid. */
    public boolean paid() {
        return status == CONFIRMED || status == CHECKED_IN || status == CHECKED_OUT;
    }

    /** Whether the reservation holds its room (everything but cancelled). */
    public boolean holdsRoom() {
        return status != CANCELLED;
    }

    /** Money that stays with the inn: paid minus refunded. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public Reservation confirm() {
        requireStatus("confirm", PENDING);
        return withStatus(CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        requireStatus("check in", CONFIRMED);
        return withStatus(CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        requireStatus("check out", CHECKED_IN);
        return withStatus(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancel", PENDING, CONFIRMED);
        return withStatus(CANCELLED, amountPaid, ratePlan.refundFor(amountPaid));
    }

    private void requireStatus(String operation, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Cannot " + operation + " a reservation with status " + status);
    }

    private Reservation withStatus(ReservationStatus newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, paid, newRefund);
    }
}
