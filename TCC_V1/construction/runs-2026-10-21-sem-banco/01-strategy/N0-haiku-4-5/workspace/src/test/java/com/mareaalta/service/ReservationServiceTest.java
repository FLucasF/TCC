package com.mareaalta.service;

import com.mareaalta.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {
    private ReservationService service;

    @BeforeEach
    public void setUp() {
        service = new ReservationService();
    }

    @Test
    public void testCreateReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "test@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);

        assertNotNull(res);
        assertEquals(1, res.id);
        assertEquals(101, res.roomNumber);
        assertEquals("Test Guest", res.guestName);
        assertEquals("PENDING", res.status);
        assertEquals(0, res.amountPaid);
        assertEquals(2, res.nights);
    }

    @Test
    public void testConfirmReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "test@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);

        String error = service.confirmReservation(res.id);
        assertNull(error);

        Reservation confirmed = service.getReservation(res.id);
        assertEquals("CONFIRMED", confirmed.status);
        assertEquals(confirmed.total, confirmed.amountPaid, 0.001);
    }

    @Test
    public void testCheckInAndOut() {
        Reservation res = service.createReservation(101, "Test Guest", "test@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);

        service.confirmReservation(res.id);

        String checkInError = service.checkIn(res.id);
        assertNull(checkInError);

        Reservation checkedIn = service.getReservation(res.id);
        assertEquals("CHECKED_IN", checkedIn.status);

        String checkOutError = service.checkOut(res.id);
        assertNull(checkOutError);

        Reservation checkedOut = service.getReservation(res.id);
        assertEquals("CHECKED_OUT", checkedOut.status);
    }

    @Test
    public void testCancelPendingReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "test@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);

        String error = service.cancelReservation(res.id);
        assertNull(error);

        Reservation cancelled = service.getReservation(res.id);
        assertEquals("CANCELLED", cancelled.status);
        assertEquals(0.0, cancelled.refund, 0.001);
    }

    @Test
    public void testCancelConfirmedStandardReservation() {
        Reservation res = service.createReservation(101, "Test Guest", "test@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);

        service.confirmReservation(res.id);
        service.cancelReservation(res.id);

        Reservation cancelled = service.getReservation(res.id);
        assertEquals(cancelled.total, cancelled.refund, 0.001);
    }

    @Test
    public void testCancelWeekendPackageRefund() {
        Reservation res = service.createReservation(201, "Test Guest", "test@example.com", "123.456.789-00",
            "2027-03-12", "2027-03-14", 3, "WEEKEND_PACKAGE", null);

        service.confirmReservation(res.id);
        service.cancelReservation(res.id);

        Reservation cancelled = service.getReservation(res.id);
        assertEquals(410.00, cancelled.refund, 0.001);
    }

    @Test
    public void testValidateInvalidDates() {
        String error = service.validateCreateReservation(101, "invalid-date", "2027-03-12", 2, "STANDARD", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateCheckOutBeforeCheckIn() {
        String error = service.validateCreateReservation(101, "2027-03-12", "2027-03-10", 2, "STANDARD", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateRoomNotFound() {
        String error = service.validateCreateReservation(999, "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        assertEquals("ROOM_NOT_FOUND", error);
    }

    @Test
    public void testValidateCapacityExceeded() {
        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 3, "STANDARD", null);
        assertEquals("CAPACITY_EXCEEDED", error);
    }

    @Test
    public void testValidateZeroGuests() {
        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 0, "STANDARD", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateInvalidRatePlan() {
        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 2, "INVALID_PLAN", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateCorporateWithoutCode() {
        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 2, "CORPORATE", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateCorporateWithInvalidCode() {
        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 2, "CORPORATE", "INVALID_CODE");
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateWeekendPackageInvalidDates() {
        String error = service.validateCreateReservation(201, "2027-03-10", "2027-03-12", 3, "WEEKEND_PACKAGE", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testValidateLongStayTooShort() {
        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 2, "LONG_STAY", null);
        assertEquals("INVALID_REQUEST", error);
    }

    @Test
    public void testRoomOverlap() {
        service.createReservation(101, "Guest 1", "test1@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);

        String error = service.validateCreateReservation(101, "2027-03-11", "2027-03-13", 2, "STANDARD", null);
        assertEquals("ROOM_UNAVAILABLE", error);
    }

    @Test
    public void testCancelledReservationDoesntBlock() {
        Reservation res1 = service.createReservation(101, "Guest 1", "test1@example.com", "123.456.789-00",
            "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        service.cancelReservation(res1.id);

        String error = service.validateCreateReservation(101, "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        assertNull(error);
    }
}
