package com.pousadamarealta;

import com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testStandardReservationPricing() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
    }

    @Test
    void testHighSeasonPricing() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation reservation = new Reservation(
            1L, "201", RoomType.DELUXE,
            "Maria Santos", "maria@example.com", "98765432109",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    void testReservationConfirmation() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(reservation.getTotal(), reservation.getAmountPaid());
    }

    @Test
    void testReservationCancellationWithRefund() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(reservation.getAmountPaid(), reservation.getRefund());
    }

    @Test
    void testNonRefundableCancellation() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    void testPendingCancellationNoRefund() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    void testInvalidStateTransitions() {
        Reservation reservation = new Reservation(
            1L, "101", RoomType.STANDARD,
            "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(IllegalStateException.class, reservation::checkIn);

        reservation.confirm();
        assertThrows(IllegalStateException.class, reservation::confirm);

        reservation.checkIn();
        assertThrows(IllegalStateException.class, reservation::cancel);

        reservation.checkOut();
        assertThrows(IllegalStateException.class, reservation::cancel);
    }
}
