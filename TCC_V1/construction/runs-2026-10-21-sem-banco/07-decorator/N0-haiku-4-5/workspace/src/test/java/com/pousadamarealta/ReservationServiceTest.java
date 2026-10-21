package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {
    private ReservationService service;
    private MessageService messageService;

    @BeforeEach
    public void setUp() {
        messageService = new MessageService(false);
        service = new ReservationService(messageService);
    }

    @Test
    public void testCreateReservation() {
        Reservation reservation = service.createReservation(
                101,
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        assertEquals(1, reservation.getId());
        assertEquals(101, reservation.getRoomNumber());
        assertEquals("João Silva", reservation.getGuestName());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(499.80, reservation.getSubtotal());
        assertEquals(0, reservation.getDiscount());
        assertEquals(499.80, reservation.getTotal());
    }

    @Test
    public void testHighSeasonPricing() {
        Reservation reservation = service.createReservation(
                101,
                "Ana",
                "ana@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(599.76, reservation.getSubtotal());
        assertEquals(0, reservation.getDiscount());
        assertEquals(599.76, reservation.getTotal());
    }

    @Test
    public void testNonRefundablePlan() {
        Reservation reservation = service.createReservation(
                201,
                "Maria",
                "maria@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.NON_REFUNDABLE
        );

        assertEquals(2, reservation.getNights());
        assertEquals(779.80, reservation.getSubtotal());
        assertEquals(116.97, reservation.getDiscount());
        assertEquals(662.83, reservation.getTotal());
    }

    @Test
    public void testConfirmReservation() {
        Reservation reservation = service.createReservation(
                101,
                "João",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        Reservation confirmed = service.confirmReservation(reservation.getId(), reservation.getTotal());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(499.80, confirmed.getAmountPaid());
        assertEquals(2, messageService.getMessages().stream()
                .filter(m -> m.getStatus().toString().equals("SENT"))
                .count());
    }

    @Test
    public void testRoomUnavailable() {
        service.createReservation(
                101,
                "João",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        assertThrows(IllegalArgumentException.class, () -> {
            service.createReservation(
                    101,
                    "Maria",
                    "maria@example.com",
                    "12345678901",
                    LocalDate.of(2027, 3, 11),
                    LocalDate.of(2027, 3, 13),
                    2,
                    RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCapacityExceeded() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createReservation(
                    101,
                    "João",
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
    public void testCancellationStandardPlan() {
        Reservation reservation = service.createReservation(
                101,
                "João",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        service.confirmReservation(reservation.getId(), reservation.getTotal());

        Reservation cancelled = service.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(499.80, cancelled.getRefund());
    }

    @Test
    public void testCancellationNonRefundablePlan() {
        Reservation reservation = service.createReservation(
                101,
                "João",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.NON_REFUNDABLE
        );

        service.confirmReservation(reservation.getId(), reservation.getTotal());

        Reservation cancelled = service.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getRefund());
    }

    @Test
    public void testCheckIn() {
        Reservation reservation = service.createReservation(
                101,
                "João",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        service.confirmReservation(reservation.getId(), reservation.getTotal());
        Reservation checkedIn = service.checkIn(reservation.getId());

        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCheckOut() {
        Reservation reservation = service.createReservation(
                101,
                "João",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
        );

        service.confirmReservation(reservation.getId(), reservation.getTotal());
        service.checkIn(reservation.getId());
        String comprovante = service.checkOut(reservation.getId());

        assertTrue(comprovante.contains("Comprovante de reserva"));
        assertTrue(comprovante.contains("João"));
    }
}
