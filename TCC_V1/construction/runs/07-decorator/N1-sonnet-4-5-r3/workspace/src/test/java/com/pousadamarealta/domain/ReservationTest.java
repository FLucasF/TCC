package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    @Test
    public void shouldCalculateStandardPriceInLowSeason() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(0, new BigDecimal("499.80").compareTo(reservation.calculateSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(reservation.calculateDiscount()));
        assertEquals(0, new BigDecimal("499.80").compareTo(reservation.calculateTotal()));
    }

    @Test
    public void shouldCalculateHighSeasonPriceInJuly() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "João Santos", "joao@example.com", "98765432109",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.calculateSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(reservation.calculateDiscount()));
        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.calculateTotal()));
    }

    @Test
    public void shouldCalculateHighSeasonPriceInDecember() {
        Reservation reservation = Reservation.createNew(
            "201", RoomType.DELUXE, new BigDecimal("389.90"),
            "Maria Costa", "maria@example.com", "11122233344",
            LocalDate.of(2026, 12, 20),
            LocalDate.of(2026, 12, 22),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(0, new BigDecimal("935.76").compareTo(reservation.calculateSubtotal()));
    }

    @Test
    public void shouldApplyNonRefundableDiscount() {
        Reservation reservation = Reservation.createNew(
            "201", RoomType.DELUXE, new BigDecimal("389.90"),
            "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        assertEquals(0, new BigDecimal("779.80").compareTo(reservation.calculateSubtotal()));
        assertEquals(0, new BigDecimal("116.97").compareTo(reservation.calculateDiscount()));
        assertEquals(0, new BigDecimal("662.83").compareTo(reservation.calculateTotal()));
    }

    @Test
    public void shouldTransitionFromPendingToConfirmed() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(reservation.getAmountPaid()));

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(0, new BigDecimal("499.80").compareTo(reservation.getAmountPaid()));
    }

    @Test
    public void shouldTransitionThroughAllStates() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        reservation.confirm();
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());

        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    public void shouldCancelWithRefundForStandardPlan() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm();
        assertEquals(0, new BigDecimal("499.80").compareTo(reservation.getAmountPaid()));

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(0, new BigDecimal("499.80").compareTo(reservation.getRefund()));
        assertEquals(0, BigDecimal.ZERO.compareTo(reservation.getNetRevenue()));
    }

    @Test
    public void shouldCancelWithoutRefundForNonRefundablePlan() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        BigDecimal amountPaid = reservation.getAmountPaid();

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(reservation.getRefund()));
        assertEquals(0, amountPaid.compareTo(reservation.getNetRevenue()));
    }

    @Test
    public void shouldNotAllowInvalidTransitions() {
        Reservation reservation = Reservation.createNew(
            "101", RoomType.STANDARD, new BigDecimal("249.90"),
            "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(IllegalStateException.class, reservation::checkIn);

        reservation.confirm();
        assertThrows(IllegalStateException.class, reservation::confirm);

        reservation.checkIn();
        assertThrows(IllegalStateException.class, reservation::cancel);
    }
}
