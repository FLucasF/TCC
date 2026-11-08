package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }

    public Reservation confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        return with(ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation checkInGuest() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        return with(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkOutGuest() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out de quem fez check-in");
        return with(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar uma reserva pendente ou confirmada");
        return with(ReservationStatus.CANCELLED, amountPaid, ratePlan.refundOf(amountPaid));
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw DomainException.notAllowed(message);
    }
}
