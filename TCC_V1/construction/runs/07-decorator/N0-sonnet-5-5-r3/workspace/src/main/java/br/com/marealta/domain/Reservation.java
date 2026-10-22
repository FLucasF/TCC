package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
        BigDecimal subtotal, BigDecimal discount, BigDecimal total, Status status,
        BigDecimal amountPaid, BigDecimal refund) {

    public Reservation with(Status newStatus, BigDecimal paid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, paid, newRefund);
    }
}
