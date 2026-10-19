package com.pousadameraalta.service;

import com.pousadameraalta.model.RatePlan;
import com.pousadameraalta.model.Reservation;
import com.pousadameraalta.model.ReservationStatus;
import com.pousadameraalta.repository.DataStore;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationServiceTest {
    private ReservationService service;
    private DataStore dataStore;

    @Before
    public void setUp() {
        dataStore = DataStore.getInstance();
        dataStore.reset();
        service = new ReservationService();
    }

    @Test
    public void testCreateReservationWithCpf() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn, checkOut, 2, RatePlan.STANDARD
        );

        assertNotNull(reservation);
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("779.80"), reservation.getTotal());
    }

    @Test
    public void testCreateReservationInvalidRoom() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        try {
            service.createReservation(
                    999, "John Doe", "john@example.com",
                    "11122233344", null, null, null, null,
                    checkIn, checkOut, 1, RatePlan.STANDARD
            );
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("ROOM_NOT_FOUND", e.getMessage());
        }
    }

    @Test
    public void testCreateReservationCapacityExceeded() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        try {
            service.createReservation(
                    101, "John Doe", "john@example.com",
                    "11122233344", null, null, null, null,
                    checkIn, checkOut, 5, RatePlan.STANDARD
            );
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("CAPACITY_EXCEEDED", e.getMessage());
        }
    }

    @Test
    public void testCreateReservationInvalidDates() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 10);

        try {
            service.createReservation(
                    201, "John Doe", "john@example.com",
                    "11122233344", null, null, null, null,
                    checkIn, checkOut, 1, RatePlan.STANDARD
            );
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("INVALID_REQUEST", e.getMessage());
        }
    }

    @Test
    public void testConfirmReservation() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn, checkOut, 2, RatePlan.STANDARD
        );

        Reservation confirmed = service.confirmReservation(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(new BigDecimal("779.80"), confirmed.getAmountPaid());
    }

    @Test
    public void testCheckIn() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn, checkOut, 2, RatePlan.STANDARD
        );

        service.confirmReservation(reservation.getId());
        Reservation checkedIn = service.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCancelReservationPending() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn, checkOut, 2, RatePlan.STANDARD
        );

        Reservation cancelled = service.cancelReservation(reservation.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, cancelled.getRefund().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCancelReservationConfirmedStandard() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn, checkOut, 2, RatePlan.STANDARD
        );

        service.confirmReservation(reservation.getId());
        Reservation cancelled = service.cancelReservation(reservation.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(new BigDecimal("779.80"), cancelled.getRefund());
    }

    @Test
    public void testCancelReservationConfirmedNonRefundable() throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn, checkOut, 2, RatePlan.NON_REFUNDABLE
        );

        service.confirmReservation(reservation.getId());
        Reservation cancelled = service.cancelReservation(reservation.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, cancelled.getRefund().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testRoomUnavailable() throws Exception {
        LocalDate checkIn1 = LocalDate.of(2027, 3, 10);
        LocalDate checkOut1 = LocalDate.of(2027, 3, 12);

        Reservation reservation1 = service.createReservation(
                201, "Carla Dias", "carla@example.com",
                "11122233344", null, null, null, null,
                checkIn1, checkOut1, 2, RatePlan.STANDARD
        );
        service.confirmReservation(reservation1.getId());

        LocalDate checkIn2 = LocalDate.of(2027, 3, 11);
        LocalDate checkOut2 = LocalDate.of(2027, 3, 13);

        try {
            service.createReservation(
                    201, "John Doe", "john@example.com",
                    "22233344455", null, null, null, null,
                    checkIn2, checkOut2, 2, RatePlan.STANDARD
            );
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("ROOM_UNAVAILABLE", e.getMessage());
        }
    }
}
