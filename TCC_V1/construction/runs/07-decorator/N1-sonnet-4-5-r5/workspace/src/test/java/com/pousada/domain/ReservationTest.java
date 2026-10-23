package com.pousada.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void shouldCalculateRegularSeasonPrice() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
    }

    @Test
    void shouldCalculateHighSeasonPrice() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void shouldApplyNonRefundableDiscount() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE, new BigDecimal("249.90")
        );

        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("74.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("424.83"), reservation.getTotal());
    }

    @Test
    void shouldDetectJulyAsHighSeason() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 7, 1),
            LocalDate.of(2027, 7, 31),
            2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        BigDecimal expected = new BigDecimal("249.90")
            .multiply(new BigDecimal("1.20"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN)
            .multiply(new BigDecimal("30"));

        assertEquals(expected, reservation.getSubtotal());
    }

    @Test
    void shouldDetectDecember15OnwardsAsHighSeason() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 12, 15),
            LocalDate.of(2027, 12, 20),
            2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        BigDecimal nightlyHighSeason = new BigDecimal("249.90")
            .multiply(new BigDecimal("1.20"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
        BigDecimal expected = nightlyHighSeason.multiply(new BigDecimal("5"));

        assertEquals(expected, reservation.getSubtotal());
    }

    @Test
    void shouldConfirmReservation() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), reservation.getAmountPaid());
    }

    @Test
    void shouldCancelWithRefundForStandardPlan() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), reservation.getRefund());
    }

    @Test
    void shouldCancelWithoutRefundForNonRefundablePlan() {
        Reservation reservation = new Reservation(
            "101", "Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE, new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }
}
