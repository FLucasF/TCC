package com.pousadamarealta;

import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;

public class ReservationServiceTest {

    private ReservationService service;

    @Before
    public void setUp() {
        ReservationService.reset();
        service = ReservationService.getInstance();
    }

    @Test
    public void testCreateReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        assertNotNull(res);
        assertEquals(101, res.getRoomNumber());
        assertEquals("Test Guest", res.getGuestName());
        assertEquals(2, res.getNights());
        assertEquals(ReservationStatus.PENDING, res.getStatus());
    }

    @Test
    public void testHighSeasonPricing() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 1, "STANDARD");

        assertNotNull(res);
        assertEquals(599.76, res.getSubtotal(), 0.01);
        assertEquals(0.0, res.getDiscount(), 0.01);
        assertEquals(599.76, res.getTotal(), 0.01);
    }

    @Test
    public void testNonRefundableDiscount() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 1, "NON_REFUNDABLE");

        assertNotNull(res);
        assertEquals(599.76, res.getSubtotal(), 0.01);
        assertEquals(89.964, res.getDiscount(), 0.01);
    }

    @Test
    public void testRoomUnavailable() {
        service.createReservation(101, "Guest 1", "guest1@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        Reservation res = service.createReservation(101, "Guest 2", "guest2@example.com", "12345678902",
            LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13), 1, "STANDARD");

        assertNull(res);
    }

    @Test
    public void testCapacityExceeded() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 3, "STANDARD");

        assertNull(res);
    }

    @Test
    public void testConfirmReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        Reservation confirmed = service.confirmReservation(res.getId(), res.getTotal());

        assertNotNull(confirmed);
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(res.getTotal(), confirmed.getAmountPaid(), 0.01);
    }

    @Test
    public void testCheckInCheckOut() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        service.confirmReservation(res.getId(), res.getTotal());
        Reservation checkedIn = service.checkIn(res.getId());

        assertNotNull(checkedIn);
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = service.checkOut(res.getId());
        assertNotNull(checkedOut);
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancelPendingReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        Reservation cancelled = service.cancelReservation(res.getId());

        assertNotNull(cancelled);
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    public void testCancelConfirmedReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        service.confirmReservation(res.getId(), res.getTotal());
        Reservation cancelled = service.cancelReservation(res.getId());

        assertNotNull(cancelled);
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(res.getTotal(), cancelled.getRefund(), 0.01);
    }

    @Test
    public void testCannotCancelCheckedIn() {
        Reservation res = service.createReservation(101, "Test Guest", "guest@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD");

        service.confirmReservation(res.getId(), res.getTotal());
        service.checkIn(res.getId());

        Reservation cancelled = service.cancelReservation(res.getId());
        assertNull(cancelled);
    }
}
