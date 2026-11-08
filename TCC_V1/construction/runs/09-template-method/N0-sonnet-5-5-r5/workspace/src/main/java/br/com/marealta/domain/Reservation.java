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
        Status status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public Reservation withStatus(Status newStatus) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, amountPaid, refund);
    }

    public Reservation confirmed() {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, Status.CONFIRMED, total, refund);
    }

    public Reservation cancelled(BigDecimal refundAmount) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, Status.CANCELLED, amountPaid, refundAmount);
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }
}
