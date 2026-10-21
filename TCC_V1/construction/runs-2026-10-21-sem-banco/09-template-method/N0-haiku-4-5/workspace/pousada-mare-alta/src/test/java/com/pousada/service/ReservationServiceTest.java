package com.pousada.service;

import com.pousada.model.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    private ReservationService reservationService;
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        reservationRepository = new ReservationRepository();
        roomRepository = new RoomRepository();
        reservationService = new ReservationService(reservationRepository, roomRepository);
    }

    @Test
    void testCreateReservation() {
        Reservation r = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(1, r.getId());
        assertEquals(101, r.getRoomNumber());
        assertEquals("João Silva", r.getGuestName());
        assertEquals(2, r.getNights());
        assertEquals(ReservationStatus.PENDING, r.getStatus());
        assertEquals(BigDecimal.ZERO, r.getAmountPaid());
    }

    @Test
    void testHighSeasonPricing() {
        Reservation r = reservationService.createReservation(
            101, "Maria", "maria@example.com", "11122233344",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        // Standard room is R$ 249.90, high season is +20% = R$ 299.88 per night
        // 2 nights = R$ 599.76
        assertEquals(new BigDecimal("599.76"), r.getSubtotal());
        assertEquals(new BigDecimal("599.76"), r.getTotal());
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation r = reservationService.createReservation(
            101, "Ana", "ana@example.com", "11122233344",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        // 15% discount on 599.76 = 89.964, rounds to 89.96
        // Total = 599.76 - 89.96 = 509.80
        assertEquals(new BigDecimal("599.76"), r.getSubtotal());
        assertEquals(new BigDecimal("89.96"), r.getDiscount());
        assertEquals(new BigDecimal("509.80"), r.getTotal());
    }

    @Test
    void testConfirmReservation() {
        Reservation r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(r.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(r.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    void testCheckIn() {
        Reservation r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(r.getId());
        Reservation checkedIn = reservationService.checkIn(r.getId());

        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    void testCheckOut() {
        Reservation r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(r.getId());
        reservationService.checkIn(r.getId());
        Reservation checkedOut = reservationService.checkOut(r.getId());

        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    void testRoomUnavailable() {
        reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(
                101, "Maria", "maria@example.com", "11122233344",
                LocalDate.of(2027, 7, 11),
                LocalDate.of(2027, 7, 13),
                2, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(
                101, "João", "joao@example.com", "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                3, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCancelReservationStandardRefund() {
        Reservation r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(r.getId());
        Reservation cancelled = reservationService.cancelReservation(r.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    void testCancelReservationNonRefundable() {
        Reservation r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(r.getId());
        Reservation cancelled = reservationService.cancelReservation(r.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }
}
