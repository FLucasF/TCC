package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code refund} é null enquanto a reserva não foi cancelada. */
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

    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, amountPaid, refund);
    }

    public Reservation withPayment(ReservationStatus newStatus, BigDecimal paid) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, paid, refund);
    }

    public Reservation withCancellation(BigDecimal refunded) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, ReservationStatus.CANCELLED, amountPaid, refunded);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }
}
