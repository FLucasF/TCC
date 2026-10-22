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

    public Reservation confirmed() {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, Operation.CONFIRM.apply(status), total, refund);
    }

    public Reservation checkedIn() {
        return withStatus(Operation.CHECK_IN.apply(status), amountPaid, refund);
    }

    public Reservation checkedOut() {
        return withStatus(Operation.CHECK_OUT.apply(status), amountPaid, refund);
    }

    public Reservation cancelled() {
        ReservationStatus next = Operation.CANCEL.apply(status);
        return withStatus(next, amountPaid, ratePlan.refundFor(amountPaid));
    }

    /** Money the hotel keeps: paid minus refunded. */
    public BigDecimal revenue() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    private Reservation withStatus(ReservationStatus newStatus, BigDecimal paid, BigDecimal refunded) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, newStatus, paid, refunded);
    }
}
