package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testReservationHighSeasonWithDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                checkIn, checkOut, 2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(new BigDecimal("0.00"), reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void testReservationNonRefundableDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = new Reservation(
                1, "201", RoomType.DELUXE, "Test Guest", "test@example.com", "12345678900",
                checkIn, checkOut, 2, RatePlan.NON_REFUNDABLE
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(new BigDecimal("0.00"), reservation.getAmountPaid());

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(reservation.getTotal(), reservation.getAmountPaid());
    }

    @Test
    void testCancelPendingReservation() {
        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, RatePlan.STANDARD
        );

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("0.00"), reservation.getRefund());
    }

    @Test
    void testCancelConfirmedReservationStandard() {
        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, RatePlan.STANDARD
        );

        reservation.confirm();
        BigDecimal amountPaid = reservation.getAmountPaid();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(amountPaid, reservation.getRefund());
    }

    @Test
    void testCancelConfirmedReservationNonRefundable() {
        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("0.00"), reservation.getRefund());
    }

    @Test
    void testCheckInAndCheckOut() {
        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void testOccupiesNight() {
        Reservation reservation = new Reservation(
                1, "101", RoomType.STANDARD, "Test Guest", "test@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, RatePlan.STANDARD
        );

        assertTrue(reservation.occupiesNight(LocalDate.of(2027, 3, 10)));
        assertTrue(reservation.occupiesNight(LocalDate.of(2027, 3, 11)));
        assertFalse(reservation.occupiesNight(LocalDate.of(2027, 3, 12)));
        assertFalse(reservation.occupiesNight(LocalDate.of(2027, 3, 9)));
    }
}
