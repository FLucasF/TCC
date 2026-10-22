package com.pousadamarealta;

import com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void testCreateReservation() {
        Reservation reservation = Reservation.create(
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            calculator
        );

        assertNull(reservation.id());
        assertEquals("201", reservation.roomNumber());
        assertEquals(ReservationStatus.PENDING, reservation.status());
        assertEquals(2, reservation.nights());
        assertEquals(new BigDecimal("779.80"), reservation.subtotal());
        assertEquals(0, reservation.discount().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("779.80"), reservation.total());
        assertEquals(0, reservation.amountPaid().compareTo(BigDecimal.ZERO));
        assertNull(reservation.refund());
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = Reservation.create(
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            calculator
        );

        reservation.confirm();
        assertEquals(ReservationStatus.CONFIRMED, reservation.status());
        assertEquals(reservation.total(), reservation.amountPaid());
    }

    @Test
    void testCannotConfirmConfirmedReservation() {
        Reservation reservation = Reservation.create(
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            calculator
        );

        reservation.confirm();
        assertThrows(IllegalStateException.class, reservation::confirm);
    }

    @Test
    void testCancelStandardReservationWithRefund() {
        Reservation reservation = Reservation.create(
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            calculator
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertEquals(reservation.amountPaid(), reservation.refund());
    }

    @Test
    void testCancelNonRefundableReservationNoRefund() {
        Reservation reservation = Reservation.create(
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE,
            calculator
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertEquals(BigDecimal.ZERO, reservation.refund());
    }

    @Test
    void testOccupiesRoom() {
        Reservation reservation = Reservation.create(
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            calculator
        );

        assertTrue(reservation.occupiesRoom(LocalDate.of(2027, 3, 10)));
        assertTrue(reservation.occupiesRoom(LocalDate.of(2027, 3, 11)));
        assertFalse(reservation.occupiesRoom(LocalDate.of(2027, 3, 12)));
        assertFalse(reservation.occupiesRoom(LocalDate.of(2027, 3, 9)));
    }
}
