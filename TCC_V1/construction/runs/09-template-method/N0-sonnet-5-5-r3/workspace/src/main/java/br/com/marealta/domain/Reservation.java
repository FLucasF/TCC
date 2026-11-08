package br.com.marealta.domain;

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

    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, amountPaid, refund);
    }

    public Reservation confirmed() {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation cancelled() {
        BigDecimal refunded = ratePlan.refundable() ? amountPaid : Money.ZERO;
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, ReservationStatus.CANCELLED, amountPaid, refunded);
    }

    /** Money that stays with the inn: paid minus refunded. */
    public BigDecimal amountKept() {
        return amountPaid.subtract(refund == null ? Money.ZERO : refund);
    }

    public boolean isPaid() {
        return ReservationStatus.PAID.contains(status);
    }
}
