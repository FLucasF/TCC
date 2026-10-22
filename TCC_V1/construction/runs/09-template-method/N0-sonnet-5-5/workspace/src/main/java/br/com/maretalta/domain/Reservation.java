package br.com.maretalta.domain;

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

    public Reservation withPayment(BigDecimal paid) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, paid, refund);
    }

    public Reservation withRefund(BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, newRefund);
    }

    public Room room() {
        return Room.find(roomNumber).orElseThrow();
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }
}
