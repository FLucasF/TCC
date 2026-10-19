package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {
    private ReservationService reservationService;

    @BeforeEach
    public void setUp() {
        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService(false);
        reservationService = new ReservationService(roomService, messageService);
    }

    @Test
    public void testCreateReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(1, reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());
    }

    @Test
    public void testRoomNotFound() {
        assertThrows(ReservationService.RoomNotFoundException.class, () -> {
            reservationService.createReservation(
                "999",
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCapacityExceeded() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.createReservation(
                "101",
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                5,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testInvalidGuestCount() {
        assertThrows(ReservationService.InvalidRequestException.class, () -> {
            reservationService.createReservation(
                "101",
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                0,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testInvalidDates() {
        assertThrows(ReservationService.InvalidRequestException.class, () -> {
            reservationService.createReservation(
                "101",
                "John Doe",
                "john@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 10),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testRoomUnavailable() {
        reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 15),
            2,
            RatePlan.STANDARD
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                "101",
                "Jane Doe",
                "jane@example.com",
                "98765432109",
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 17),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    public void testCancelStandardReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    public void testCancelNonRefundableReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    public void testCheckInAndCheckOut() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testOperationNotAllowed() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertThrows(ReservationService.OperationNotAllowedException.class, () -> {
            reservationService.checkIn(reservation.getId());
        });
    }

    @Test
    public void testCancelledReservationDoesNotBlockRoom() {
        Reservation reservation1 = reservationService.createReservation(
            "101",
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 15),
            2,
            RatePlan.STANDARD
        );

        reservationService.cancelReservation(reservation1.getId());

        assertDoesNotThrow(() -> {
            reservationService.createReservation(
                "101",
                "Jane Doe",
                "jane@example.com",
                "98765432109",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 15),
                2,
                RatePlan.STANDARD
            );
        });
    }
}
