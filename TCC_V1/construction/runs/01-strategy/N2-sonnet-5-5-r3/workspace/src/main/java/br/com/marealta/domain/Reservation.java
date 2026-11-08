package br.com.marealta.domain;

import br.com.marealta.DomainException;
import br.com.marealta.plan.RatePlan;

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
        String ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public Reservation confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        return withStatus(ReservationStatus.CHECKED_IN);
    }

    public Reservation endStay() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        return withStatus(ReservationStatus.CHECKED_OUT);
    }

    public Reservation cancel(RatePlan plan) {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, ReservationStatus.CANCELLED, amountPaid,
                plan.refund(amountPaid));
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, status, amountPaid, refund);
    }

    private Reservation withStatus(ReservationStatus s) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, s, amountPaid, refund);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw DomainException.notAllowed(message);
    }
}
