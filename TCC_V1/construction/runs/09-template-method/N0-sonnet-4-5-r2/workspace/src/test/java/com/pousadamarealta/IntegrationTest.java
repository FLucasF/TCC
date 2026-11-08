package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.Database;
import com.pousadamarealta.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {
    private ReservationService reservationService;
    private static int testCounter = 0;

    @BeforeEach
    public void setup() {
        testCounter++;
        Database.initialize("jdbc:h2:mem:test" + testCounter + ";DB_CLOSE_DELAY=-1", "sa", "");
        reservationService = new ReservationService();
    }

    @Test
    public void testCreateAndConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertNotNull(reservation.getId());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    public void testNonRefundableDiscount() {
        Reservation reservation = reservationService.createReservation(
            "201", "Maria Santos", "maria@email.com", "98765432100",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    public void testRoomUnavailableConflict() {
        reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                "101", "Maria Santos", "maria@email.com", "98765432100",
                LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13),
                1, RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCapacityExceeded() {
        assertThrows(CapacityExceededException.class, () -> {
            reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                5, RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCancellationWithStandardPlan() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    public void testCancellationWithNonRefundable() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    public void testFullReservationFlow() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        reservation = reservationService.confirmReservation(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());

        reservation = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    public void testInvalidOperations() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(OperationNotAllowedException.class, () -> {
            reservationService.checkIn(reservation.getId());
        });

        reservationService.confirmReservation(reservation.getId());

        assertThrows(OperationNotAllowedException.class, () -> {
            reservationService.confirmReservation(reservation.getId());
        });
    }
}
