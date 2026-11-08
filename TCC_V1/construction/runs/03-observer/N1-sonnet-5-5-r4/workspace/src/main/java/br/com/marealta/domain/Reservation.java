package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.marealta.domain.ReservationStatus.*;

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
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan) {
        Quote quote = Quote.of(room, checkIn, checkOut, plan);
        return new Reservation(null, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                plan, quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public Reservation confirmed() {
        requireStatus("confirmar", PENDING);
        return with(CONFIRMED, total, refund);
    }

    public Reservation checkedIn() {
        requireStatus("fazer check-in de", CONFIRMED);
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkedOut() {
        requireStatus("fazer check-out de", CHECKED_IN);
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancelled() {
        requireStatus("cancelar", PENDING, CONFIRMED);
        return with(CANCELLED, amountPaid, ratePlan.refundOf(amountPaid));
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean wasPaid() {
        return status == CONFIRMED || status == CHECKED_IN || status == CHECKED_OUT;
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) {
                return;
            }
        }
        throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível " + action + " uma reserva com status " + status);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
