package com.pousadamarealta.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testCalculateSubtotalLowSeason() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    void testCalculateSubtotalHighSeasonJuly() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testCalculateDiscountNonRefundable() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = reservation.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("74.97"), discount);
    }

    @Test
    void testCalculateDiscountStandard() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = reservation.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        BigDecimal total = new BigDecimal("499.80");
        reservation.confirmWithTotal(total);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(total, reservation.getAmountPaid());
    }

    @Test
    void testCancelConfirmedReservationStandardPlan() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirmWithTotal(new BigDecimal("499.80"));
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), reservation.getRefund());
    }

    @Test
    void testCancelConfirmedReservationNonRefundable() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirmWithTotal(new BigDecimal("424.83"));
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    void testCancelPendingReservation() {
        Reservation reservation = new Reservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }
}
