package com.pousada.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testStandardRoomLowSeason() {
        Reservation reservation = new Reservation(
                1L, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
    }

    @Test
    void testStandardRoomHighSeason() {
        Reservation reservation = new Reservation(
                1L, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void testNonRefundablePlan() {
        Reservation reservation = new Reservation(
                1L, "201", RoomType.DELUXE, "Maria Santos", "maria@example.com", "98765432109",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                3, RatePlan.NON_REFUNDABLE, new BigDecimal("389.90")
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = new Reservation(
                1L, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), reservation.getAmountPaid());
    }

    @Test
    void testCancelRefundable() {
        Reservation reservation = new Reservation(
                1L, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), reservation.getRefund());
        assertEquals(new BigDecimal("0.00"), reservation.getNetAmount());
    }

    @Test
    void testCancelNonRefundable() {
        Reservation reservation = new Reservation(
                1L, "201", RoomType.DELUXE, "Maria Santos", "maria@example.com", "98765432109",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                3, RatePlan.NON_REFUNDABLE, new BigDecimal("389.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
        assertEquals(new BigDecimal("662.83"), reservation.getNetAmount());
    }

    @Test
    void testHighSeasonDecember() {
        Reservation reservation = new Reservation(
                1L, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 12, 15), LocalDate.of(2027, 12, 17),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
    }

    @Test
    void testHighSeasonJanuary() {
        Reservation reservation = new Reservation(
                1L, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 12),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
    }
}
