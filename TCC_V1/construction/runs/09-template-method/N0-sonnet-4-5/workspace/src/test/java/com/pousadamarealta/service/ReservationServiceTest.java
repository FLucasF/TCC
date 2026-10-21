package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    private ReservationService reservationService;
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();

        reservationService = new ReservationService(
                reservationRepository, roomRepository, messageRepository, pricingService);
    }

    @Test
    void testCreateReservationStandard() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD);

        assertEquals(1, reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(0, reservation.getDiscount().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
    }

    @Test
    void testCreateReservationHighSeason() {
        Reservation reservation = reservationService.createReservation(
                "101", "Maria Santos", "maria@email.com", "98765432100",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                1, RatePlan.STANDARD);

        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void testCreateReservationNonRefundable() {
        Reservation reservation = reservationService.createReservation(
                "201", "Carlos Lima", "carlos@email.com", "11122233344",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    void testCreateReservationInvalidDates() {
        assertThrows(InvalidRequestException.class, () ->
                reservationService.createReservation(
                        "101", "João Silva", "joao@email.com", "12345678900",
                        LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 10),
                        2, RatePlan.STANDARD));
    }

    @Test
    void testCreateReservationRoomNotFound() {
        assertThrows(RoomNotFoundException.class, () ->
                reservationService.createReservation(
                        "999", "João Silva", "joao@email.com", "12345678900",
                        LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                        2, RatePlan.STANDARD));
    }

    @Test
    void testCreateReservationCapacityExceeded() {
        assertThrows(CapacityExceededException.class, () ->
                reservationService.createReservation(
                        "101", "João Silva", "joao@email.com", "12345678900",
                        LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                        5, RatePlan.STANDARD));
    }

    @Test
    void testCreateReservationRoomUnavailable() {
        reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 15),
                2, RatePlan.STANDARD);

        assertThrows(RoomUnavailableException.class, () ->
                reservationService.createReservation(
                        "101", "Maria Santos", "maria@email.com", "98765432100",
                        LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 17),
                        2, RatePlan.STANDARD));
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD);

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    void testCancelReservationStandard() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD);

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    void testCancelReservationNonRefundable() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE);

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    void testCheckInAndCheckOut() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD);

        reservationService.confirmReservation(reservation.getId());
        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        reservationService.checkOut(reservation.getId());
        Reservation checkedOut = reservationService.getReservation(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    void testCheckInNotAllowed() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD);

        assertThrows(OperationNotAllowedException.class, () ->
                reservationService.checkIn(reservation.getId()));
    }
}
