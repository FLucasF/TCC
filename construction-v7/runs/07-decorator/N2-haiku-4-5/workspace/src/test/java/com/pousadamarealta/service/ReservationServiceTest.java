package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationServiceTest {
    private RoomService roomService;
    private ReservationService reservationService;

    @Before
    public void setUp() {
        roomService = new RoomService();
        reservationService = new ReservationService(roomService);
    }

    @Test
    public void testCreateReservation() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        assertNotNull(res);
        assertEquals(101, res.getRoomNumber());
        assertEquals("John Doe", res.getGuestName());
        assertEquals(ReservationStatus.PENDING, res.getStatus());
        assertEquals(2, res.getNights());
    }

    @Test
    public void testCreateReservationCapacityExceeded() {
        try {
            reservationService.createReservation(
                101,
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                3,
                RatePlan.STANDARD
            );
            fail("Should throw exception for capacity exceeded");
        } catch (IllegalArgumentException e) {
            assertEquals("CAPACITY_EXCEEDED", e.getMessage());
        }
    }

    @Test
    public void testCreateReservationZeroGuests() {
        try {
            reservationService.createReservation(
                101,
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                0,
                RatePlan.STANDARD
            );
            fail("Should throw exception for zero guests");
        } catch (IllegalArgumentException e) {
            assertEquals("INVALID_REQUEST", e.getMessage());
        }
    }

    @Test
    public void testCreateReservationInvalidDates() {
        try {
            reservationService.createReservation(
                101,
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 12),
                LocalDate.of(2027, 7, 10),
                2,
                RatePlan.STANDARD
            );
            fail("Should throw exception for invalid dates");
        } catch (IllegalArgumentException e) {
            assertEquals("INVALID_REQUEST", e.getMessage());
        }
    }

    @Test
    public void testRoomNotFound() {
        try {
            reservationService.createReservation(
                999,
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
            );
            fail("Should throw exception for room not found");
        } catch (IllegalArgumentException e) {
            assertEquals("ROOM_NOT_FOUND", e.getMessage());
        }
    }

    @Test
    public void testRoomAlreadyBooked() {
        reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        try {
            reservationService.createReservation(
                101,
                "Jane Doe",
                "jane@example.com",
                "98765432101",
                LocalDate.of(2027, 7, 11),
                LocalDate.of(2027, 7, 13),
                2,
                RatePlan.STANDARD
            );
            fail("Should throw exception for room unavailable");
        } catch (IllegalArgumentException e) {
            assertEquals("ROOM_UNAVAILABLE", e.getMessage());
        }
    }

    @Test
    public void testCancelledReservationDoesNotBlockRoom() {
        Reservation res1 = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.cancelReservation(res1.getId());

        Reservation res2 = reservationService.createReservation(
            101,
            "Jane Doe",
            "jane@example.com",
            "98765432101",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        assertNotNull(res2);
    }

    @Test
    public void testConfirmReservation() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        BigDecimal amountPaid = new BigDecimal("599.76");
        reservationService.confirmReservation(res.getId(), amountPaid);

        Reservation confirmed = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(amountPaid, confirmed.getAmountPaid());
    }

    @Test
    public void testConfirmNonPendingReservation() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(res.getId(), new BigDecimal("599.76"));
        reservationService.checkIn(res.getId());

        try {
            reservationService.confirmReservation(res.getId(), new BigDecimal("599.76"));
            fail("Should not allow confirmation on checked-in reservation");
        } catch (IllegalArgumentException e) {
            assertEquals("OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    @Test
    public void testCheckIn() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(res.getId(), new BigDecimal("599.76"));
        reservationService.checkIn(res.getId());

        Reservation checkedIn = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCheckOut() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(res.getId(), new BigDecimal("599.76"));
        reservationService.checkIn(res.getId());
        reservationService.checkOut(res.getId());

        Reservation checkedOut = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancelConfirmedReservationStandardPlan() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        BigDecimal paid = new BigDecimal("599.76");
        reservationService.confirmReservation(res.getId(), paid);
        reservationService.cancelReservation(res.getId());

        Reservation cancelled = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(paid, cancelled.getRefund());
    }

    @Test
    public void testCancelConfirmedReservationNonRefundable() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        BigDecimal paid = new BigDecimal("509.80");
        reservationService.confirmReservation(res.getId(), paid);
        reservationService.cancelReservation(res.getId());

        Reservation cancelled = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    public void testCancelPendingReservation() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.cancelReservation(res.getId());

        Reservation cancelled = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }
}
