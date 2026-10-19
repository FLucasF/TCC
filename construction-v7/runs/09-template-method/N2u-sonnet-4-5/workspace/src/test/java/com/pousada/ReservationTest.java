package com.pousada;

import com.pousada.domain.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.NotificationService;
import com.pousada.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
    private ReservationService reservationService;
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        reservationRepository = new ReservationRepository();
        roomRepository = new RoomRepository();
        NotificationService notificationService = new NotificationService();
        reservationService = new ReservationService(
                reservationRepository, roomRepository, notificationService
        );
    }

    @Test
    void testCreateReservation() {
        Reservation reservation = reservationService.createReservation(
                101, "João Silva", "joao@email.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        assertEquals(1, reservation.getId());
        assertEquals(101, reservation.getRoomNumber());
        assertEquals("João Silva", reservation.getGuestName());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
    }

    @Test
    void testPriceCalculationStandard() {
        Reservation reservation = reservationService.createReservation(
                101, "Maria Santos", "maria@email.com", "11122233344",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        Room room = roomRepository.findByNumber(101).orElseThrow();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("499.80"), subtotal);
        assertEquals(BigDecimal.ZERO, discount);
        assertEquals(new BigDecimal("499.80"), total);
    }

    @Test
    void testPriceCalculationNonRefundable() {
        Reservation reservation = reservationService.createReservation(
                201, "Carlos Lima", "carlos@email.com", "55566677788",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE
        );

        Room room = roomRepository.findByNumber(201).orElseThrow();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("116.97"), discount);
        assertEquals(new BigDecimal("662.83"), total);
    }

    @Test
    void testHighSeasonPricing() {
        Reservation reservation = reservationService.createReservation(
                101, "Ana Costa", "ana@email.com", "99988877766",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD
        );

        Room room = roomRepository.findByNumber(101).orElseThrow();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
                101, "Pedro Souza", "pedro@email.com", "11111111111",
                LocalDate.of(2027, 4, 15), LocalDate.of(2027, 4, 17),
                1, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertTrue(confirmed.getAmountPaid().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCancelReservation() {
        Reservation reservation = reservationService.createReservation(
                101, "Julia Alves", "julia@email.com", "22222222222",
                LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 3),
                2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getRefund());
    }

    @Test
    void testCheckInCheckOut() {
        Reservation reservation = reservationService.createReservation(
                102, "Roberto Silva", "roberto@email.com", "33333333333",
                LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 2),
                1, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    void testRoomConflict() {
        reservationService.createReservation(
                101, "Cliente 1", "cliente1@email.com", "11111111111",
                LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 5),
                2, RatePlan.STANDARD
        );

        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                    101, "Cliente 2", "cliente2@email.com", "22222222222",
                    LocalDate.of(2027, 8, 3), LocalDate.of(2027, 8, 7),
                    2, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                    101, "Família Grande", "familia@email.com", "44444444444",
                    LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 3),
                    3, RatePlan.STANDARD
            );
        });
    }
}
