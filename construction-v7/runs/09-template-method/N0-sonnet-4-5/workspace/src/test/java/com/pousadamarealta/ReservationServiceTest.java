package com.pousadamarealta;

import com.pousadamarealta.model.*;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
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
                RatePlan.STANDARD
        );

        assertEquals(1, reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());
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
                RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation confirmed = reservationService.findById(reservation.getId()).orElseThrow();

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    void testCancelReservationWithRefund() {
        Reservation reservation = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.findById(reservation.getId()).orElseThrow();

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    void testCancelNonRefundableReservation() {
        Reservation reservation = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.findById(reservation.getId()).orElseThrow();

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
                RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        reservationService.checkIn(reservation.getId());

        Reservation checkedIn = reservationService.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        reservationService.checkOut(reservation.getId());

        Reservation checkedOut = reservationService.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
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
                RatePlan.STANDARD
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                    "101",
                    "Maria Santos",
                    "maria@example.com",
                    "98765432109",
                    LocalDate.of(2027, 3, 11),
                    LocalDate.of(2027, 3, 13),
                    2,
                    RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.createReservation(
                    "101",
                    "João Silva",
                    "joao@example.com",
                    "12345678901",
                    LocalDate.of(2027, 3, 10),
                    LocalDate.of(2027, 3, 12),
                    3,
                    RatePlan.STANDARD
            );
        });
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation reservation = reservationService.createReservation(
                "201",
                "Carla Dias",
                "carla@example.com",
                "11122233344",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.NON_REFUNDABLE
        );

        BigDecimal expectedDiscount = new BigDecimal("116.97");
        assertEquals(expectedDiscount, reservation.getDiscount());

        BigDecimal expectedTotal = new BigDecimal("662.83");
        assertEquals(expectedTotal, reservation.getTotal());
    }
}
