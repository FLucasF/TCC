package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void shouldCalculateSubtotalWithHighSeason() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.nights());
        assertEquals(new BigDecimal("599.76"), reservation.subtotal());
        assertEquals(new BigDecimal("0.00"), reservation.discount());
        assertEquals(new BigDecimal("599.76"), reservation.total());
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        Reservation reservation = new Reservation(
            1L, "201", RoomType.DELUXE, new BigDecimal("389.90"),
            "Maria", "maria@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        assertEquals(2, reservation.nights());
        assertEquals(new BigDecimal("779.80"), reservation.subtotal());
        assertEquals(new BigDecimal("116.97"), reservation.discount());
        assertEquals(new BigDecimal("662.83"), reservation.total());
    }

    @Test
    void shouldTransitionFromPendingToConfirmed() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.status());
        assertEquals(BigDecimal.ZERO, reservation.amountPaid());

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.status());
        assertEquals(reservation.total(), reservation.amountPaid());
    }

    @Test
    void shouldRefundOnCancellationWithStandardPlan() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertEquals(reservation.amountPaid(), reservation.refund());
    }

    @Test
    void shouldNotRefundOnCancellationWithNonRefundablePlan() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertEquals(BigDecimal.ZERO, reservation.refund());
    }
}
