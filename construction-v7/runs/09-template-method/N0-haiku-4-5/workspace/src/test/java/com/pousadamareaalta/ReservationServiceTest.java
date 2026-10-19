package com.pousadamareaalta;

import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.service.ReservationService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationServiceTest {
    private ReservationService service;

    @Before
    public void setUp() {
        service = new ReservationService();
    }

    @Test
    public void testCreateReservation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        Reservation res = service.createReservation(101, "John Doe", "john@example.com",
                "123.456.789-00", checkIn, checkOut, 2, Reservation.RatePlan.STANDARD);

        assertEquals(1, res.getId());
        assertEquals(101, res.getRoomNumber());
        assertEquals("John Doe", res.getGuestName());
        assertEquals(2, res.getNights());
        assertEquals(Reservation.Status.PENDING, res.getStatus());
        assertEquals(BigDecimal.ZERO, res.getAmountPaid());
        assertNull(res.getRefund());
    }

    @Test
    public void testCalculateNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        long nights = service.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testRoomNotFound() {
        try {
            service.createReservation(999, "John", "john@example.com", "123.456.789-00",
                    LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("ROOM_NOT_FOUND", e.getMessage());
        }
    }

    @Test
    public void testCapacityExceeded() {
        try {
            service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                    LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 5, Reservation.RatePlan.STANDARD);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("CAPACITY_EXCEEDED", e.getMessage());
        }
    }

    @Test
    public void testInvalidDates() {
        try {
            service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                    LocalDate.of(2027, 7, 12), LocalDate.of(2027, 7, 10), 2, Reservation.RatePlan.STANDARD);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("INVALID_REQUEST", e.getMessage());
        }
    }

    @Test
    public void testConfirmReservation() {
        Reservation res = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);

        service.confirmReservation(res.getId(), res.getTotal());

        Reservation confirmed = service.getReservation(res.getId());
        assertEquals(Reservation.Status.CONFIRMED, confirmed.getStatus());
        assertEquals(res.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    public void testCheckIn() {
        Reservation res = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);
        service.confirmReservation(res.getId(), res.getTotal());
        service.checkIn(res.getId());

        Reservation checkedIn = service.getReservation(res.getId());
        assertEquals(Reservation.Status.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCheckOut() {
        Reservation res = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);
        service.confirmReservation(res.getId(), res.getTotal());
        service.checkIn(res.getId());
        service.checkOut(res.getId());

        Reservation checkedOut = service.getReservation(res.getId());
        assertEquals(Reservation.Status.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancelPending() {
        Reservation res = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);

        service.cancelReservation(res.getId());

        Reservation cancelled = service.getReservation(res.getId());
        assertEquals(Reservation.Status.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getRefund());
    }

    @Test
    public void testCancelConfirmedStandard() {
        Reservation res = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);
        service.confirmReservation(res.getId(), res.getTotal());

        service.cancelReservation(res.getId());

        Reservation cancelled = service.getReservation(res.getId());
        assertEquals(Reservation.Status.CANCELLED, cancelled.getStatus());
        assertEquals(res.getTotal(), cancelled.getRefund());
    }

    @Test
    public void testCancelConfirmedNonRefundable() {
        Reservation res = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.NON_REFUNDABLE);
        service.confirmReservation(res.getId(), res.getTotal());

        service.cancelReservation(res.getId());

        Reservation cancelled = service.getReservation(res.getId());
        assertEquals(Reservation.Status.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getRefund());
    }

    @Test
    public void testRoomUnavailable() {
        service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);

        try {
            service.createReservation(101, "Jane", "jane@example.com", "987.654.321-00",
                    LocalDate.of(2027, 7, 11), LocalDate.of(2027, 7, 13), 1, Reservation.RatePlan.STANDARD);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("ROOM_UNAVAILABLE", e.getMessage());
        }
    }

    @Test
    public void testRoomAvailableAfterCancellation() {
        Reservation res1 = service.createReservation(101, "John", "john@example.com", "123.456.789-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, Reservation.RatePlan.STANDARD);
        service.cancelReservation(res1.getId());

        Reservation res2 = service.createReservation(101, "Jane", "jane@example.com", "987.654.321-00",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 1, Reservation.RatePlan.STANDARD);

        assertNotNull(res2);
    }

    @Test
    public void testGenerateReceipt() {
        Reservation res = service.createReservation(201, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, Reservation.RatePlan.STANDARD);

        String receipt = service.generateReceipt(res);

        assertTrue(receipt.contains("Comprovante de reserva nº 1"));
        assertTrue(receipt.contains("Carla Dias"));
        assertTrue(receipt.contains("111.222.333-44"));
        assertTrue(receipt.contains("201"));
        assertTrue(receipt.contains("DELUXE"));
        assertTrue(receipt.contains("10/03/2027"));
        assertTrue(receipt.contains("12/03/2027"));
        assertTrue(receipt.contains("2 diárias"));
    }
}
