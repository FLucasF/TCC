package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
    @Test
    void testReservationCalculations() {
        Reservation reservation = new Reservation(
            1, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678900",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, RatePlanType.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void testReservationWithDiscount() {
        Reservation reservation = new Reservation(
            1, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlanType.NON_REFUNDABLE
        );

        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("74.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("424.83"), reservation.getTotal());
    }

    @Test
    void testReservationStatusTransitions() {
        Reservation reservation = new Reservation(
            1, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlanType.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        reservation.confirm();
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(reservation.getTotal(), reservation.getAmountPaid());

        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void testCancellationWithRefund() {
        Reservation reservation = new Reservation(
            1, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlanType.STANDARD
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(reservation.getAmountPaid(), reservation.getRefund());
    }

    @Test
    void testCancellationNoRefund() {
        Reservation reservation = new Reservation(
            1, "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlanType.NON_REFUNDABLE
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }
}
