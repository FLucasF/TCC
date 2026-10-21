package com.pousada.service;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    private ReservationService reservationService;
    private RoomService roomService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        messageService = new MessageService();
        reservationService = new ReservationService(roomService, messageService);
    }

    @Test
    void testCreateReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            null
        );

        assertNotNull(reservation);
        assertEquals(1, reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(BusinessException.class, () -> {
            reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                3,
                RatePlan.STANDARD,
                null
            );
        });
    }

    @Test
    void testRoomUnavailable() {
        reservationService.createReservation(
            "101",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            null
        );

        assertThrows(BusinessException.class, () -> {
            reservationService.createReservation(
                "101",
                "Maria Santos",
                "maria@example.com",
                "98765432109",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                2,
                RatePlan.STANDARD,
                null
            );
        });
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            null
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
        assertEquals(1, messageService.getAllMessages().size());
    }

    @Test
    void testCancelNonRefundable() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE,
            null
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    void testCheckInAndCheckOut() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            null
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }
}
