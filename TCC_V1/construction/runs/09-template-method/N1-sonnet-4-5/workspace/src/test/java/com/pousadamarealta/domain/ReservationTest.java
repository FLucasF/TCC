package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    @Test
    public void testCalculateNightsInHighSeason() {
        Reservation reservation = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("599.76"), reservation.calculateSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.calculateDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.calculateTotal());
    }

    @Test
    public void testCalculateNightsInLowSeason() {
        Reservation reservation = Reservation.create(
                "101",
                "Maria Santos",
                "maria@example.com",
                "98765432100",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.calculateSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.calculateDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.calculateTotal());
    }

    @Test
    public void testNonRefundableDiscount() {
        Reservation reservation = Reservation.create(
                "201",
                "Pedro Costa",
                "pedro@example.com",
                "11122233344",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.NON_REFUNDABLE
        );

        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = new BigDecimal("662.83");

        assertEquals(subtotal, reservation.calculateSubtotal());
        assertEquals(discount, reservation.calculateDiscount());
        assertEquals(total, reservation.calculateTotal());
    }

    @Test
    public void testConfirmReservation() {
        Reservation reservation = Reservation.create(
                "101",
                "Ana Silva",
                "ana@example.com",
                "55566677788",
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 5, 3),
                1,
                RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), reservation.getAmountPaid());
    }

    @Test
    public void testCheckInAndCheckOut() {
        Reservation reservation = Reservation.create(
                "102",
                "Carlos Souza",
                "carlos@example.com",
                "99988877766",
                LocalDate.of(2027, 6, 1),
                LocalDate.of(2027, 6, 2),
                2,
                RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    public void testCancelWithRefund() {
        Reservation reservation = Reservation.create(
                "201",
                "Julia Lima",
                "julia@example.com",
                "44455566677",
                LocalDate.of(2027, 8, 1),
                LocalDate.of(2027, 8, 3),
                2,
                RatePlan.STANDARD
        );

        reservation.confirm();
        BigDecimal paid = reservation.getAmountPaid();

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(paid, reservation.getRefund());
    }

    @Test
    public void testCancelNonRefundable() {
        Reservation reservation = Reservation.create(
                "202",
                "Roberto Dias",
                "roberto@example.com",
                "33344455566",
                LocalDate.of(2027, 9, 1),
                LocalDate.of(2027, 9, 2),
                1,
                RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        assertTrue(reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0);

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    public void testCannotConfirmConfirmedReservation() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 10, 1),
                LocalDate.of(2027, 10, 2),
                1,
                RatePlan.STANDARD
        );

        reservation.confirm();

        assertThrows(IllegalStateException.class, () -> reservation.confirm());
    }

    @Test
    public void testCannotCheckInPendingReservation() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 10, 1),
                LocalDate.of(2027, 10, 2),
                1,
                RatePlan.STANDARD
        );

        assertThrows(IllegalStateException.class, () -> reservation.checkIn());
    }

    @Test
    public void testCannotCheckOutBeforeCheckIn() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 10, 1),
                LocalDate.of(2027, 10, 2),
                1,
                RatePlan.STANDARD
        );

        reservation.confirm();

        assertThrows(IllegalStateException.class, () -> reservation.checkOut());
    }

    @Test
    public void testCannotCancelAfterCheckIn() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 10, 1),
                LocalDate.of(2027, 10, 2),
                1,
                RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();

        assertThrows(IllegalStateException.class, () -> reservation.cancel());
    }

    @Test
    public void testTourismTax() {
        Reservation reservation = Reservation.create(
                "201",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 5, 4),
                2,
                RatePlan.STANDARD
        );

        BigDecimal expectedTax = new BigDecimal("27.00");
        assertEquals(expectedTax, reservation.calculateTourismTax());
    }

    @Test
    public void testOccupiesNight() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                1,
                RatePlan.STANDARD
        );

        assertTrue(reservation.occupiesNight(LocalDate.of(2027, 5, 10)));
        assertTrue(reservation.occupiesNight(LocalDate.of(2027, 5, 11)));
        assertFalse(reservation.occupiesNight(LocalDate.of(2027, 5, 12)));
        assertFalse(reservation.occupiesNight(LocalDate.of(2027, 5, 9)));
    }

    @Test
    public void testIsPaid() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                1,
                RatePlan.STANDARD
        );

        assertFalse(reservation.isPaid());

        reservation.confirm();
        assertTrue(reservation.isPaid());

        reservation.checkIn();
        assertTrue(reservation.isPaid());

        reservation.checkOut();
        assertTrue(reservation.isPaid());
    }

    @Test
    public void testNetRevenue() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                1,
                RatePlan.STANDARD
        );

        assertEquals(BigDecimal.ZERO, reservation.getNetRevenue());

        reservation.confirm();
        BigDecimal total = reservation.calculateTotal();
        assertEquals(total, reservation.getNetRevenue());

        reservation.cancel();
        assertEquals(0, reservation.getNetRevenue().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testHighSeasonDecember() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 12, 15),
                LocalDate.of(2027, 12, 17),
                1,
                RatePlan.STANDARD
        );

        BigDecimal expected = new BigDecimal("599.76");
        assertEquals(expected, reservation.calculateSubtotal());
    }

    @Test
    public void testHighSeasonJanuary() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 12),
                1,
                RatePlan.STANDARD
        );

        BigDecimal expected = new BigDecimal("599.76");
        assertEquals(expected, reservation.calculateSubtotal());
    }

    @Test
    public void testHighSeasonFebruary() {
        Reservation reservation = Reservation.create(
                "101",
                "Test User",
                "test@example.com",
                "12312312312",
                LocalDate.of(2028, 2, 10),
                LocalDate.of(2028, 2, 12),
                1,
                RatePlan.STANDARD
        );

        BigDecimal expected = new BigDecimal("599.76");
        assertEquals(expected, reservation.calculateSubtotal());
    }
}
