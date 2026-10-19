package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {
    private ReservationService reservationService;
    private MessageRepository messageRepository;

    @BeforeEach
    public void setUp() {
        messageRepository = new MessageRepository();
        NotificationService notificationService = new NotificationService(messageRepository);
        reservationService = new ReservationService(notificationService);
    }

    @Test
    public void testGetRooms() {
        var rooms = reservationService.getRooms();
        assertEquals(5, rooms.size());
    }

    @Test
    public void testCreateReservation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        assertNotNull(reservation);
        assertEquals("Test Guest", reservation.getGuestName());
        assertEquals(2, reservation.getNights());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
    }

    @Test
    public void testReservationConfirmation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirm(reservation.getId(), 599.76);
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(599.76, confirmed.getAmountPaid());
    }

    @Test
    public void testHighSeasonPricing() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        assertEquals(599.76, reservation.getSubtotal());
    }

    @Test
    public void testNonRefundableDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.NON_REFUNDABLE
        );

        assertEquals(599.76, reservation.getSubtotal());
        assertEquals(89.96, reservation.getDiscount());
        assertEquals(509.80, reservation.getTotal());
    }

    @Test
    public void testRoomNotAvailable() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                    101,
                    "Another Guest",
                    "other@example.com",
                    "12345678901",
                    checkIn,
                    checkOut,
                    2,
                    RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCheckIn() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirm(reservation.getId(), 599.76);
        Reservation checkedIn = reservationService.checkIn(confirmed.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCheckOut() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirm(reservation.getId(), 599.76);
        Reservation checkedIn = reservationService.checkIn(confirmed.getId());
        Reservation checkedOut = reservationService.checkOut(checkedIn.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancellation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirm(reservation.getId(), 599.76);
        Reservation cancelled = reservationService.cancel(confirmed.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(599.76, cancelled.getRefund());
    }

    @Test
    public void testCapacityExceeded() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                    101,
                    "Test Guest",
                    "test@example.com",
                    "12345678900",
                    checkIn,
                    checkOut,
                    3,
                    RatePlan.STANDARD
            );
        });
    }
}
