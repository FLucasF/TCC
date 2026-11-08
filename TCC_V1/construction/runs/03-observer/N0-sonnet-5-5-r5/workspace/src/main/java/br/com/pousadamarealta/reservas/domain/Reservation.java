package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.OPERATION_NOT_ALLOWED;

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

    public Reservation confirm() {
        require(status == ReservationStatus.PENDING, "só é possível confirmar uma reserva pendente");
        return with(ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation checkInGuest() {
        require(status == ReservationStatus.CONFIRMED, "só é possível fazer check-in de uma reserva confirmada");
        return with(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkOutGuest() {
        require(status == ReservationStatus.CHECKED_IN, "só é possível fazer check-out de uma reserva com check-in");
        return with(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    /** Plano normal devolve o que foi pago; não reembolsável (ou nada pago) não devolve nada. */
    public Reservation cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "só é possível cancelar uma reserva pendente ou confirmada");
        BigDecimal toRefund = ratePlan == RatePlan.STANDARD ? amountPaid : Money.ZERO;
        return with(ReservationStatus.CANCELLED, amountPaid, toRefund);
    }

    public void requirePaid() {
        require(status.isPaid(), "o comprovante só existe para reserva paga");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new PousadaException(OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva " + status + ": " + message);
        }
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, paid, newRefund);
    }
}
