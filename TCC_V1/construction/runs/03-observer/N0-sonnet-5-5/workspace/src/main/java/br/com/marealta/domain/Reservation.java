package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
        int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
        ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    public Reservation withStatus(ReservationStatus s) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, s, amountPaid, refund);
    }

    public Reservation withPayment(BigDecimal paid) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, paid, refund);
    }

    public Reservation withRefund(BigDecimal r) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, r);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }
}
