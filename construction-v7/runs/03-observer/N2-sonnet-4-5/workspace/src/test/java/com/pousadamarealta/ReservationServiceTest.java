package com.pousadamarealta;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.observer.ReservationEventPublisher;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        PricingService pricingService = new PricingService();
        ReservationEventPublisher eventPublisher = new ReservationEventPublisher();
        reservationService = new ReservationService(pricingService, eventPublisher);
    }

    @Test
    void shouldCreateReservation() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        assertEquals(1, reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals("João Silva", reservation.getGuestName());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
    }

    @Test
    void shouldNotCreateReservationWhenRoomUnavailable() {
        reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        ApiException exception = assertThrows(ApiException.class, () -> {
            reservationService.createReservation(
                    "101", "Maria Santos", "maria@example.com", "98765432100",
                    LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13),
                    2, RatePlan.STANDARD
            );
        });

        assertEquals("ROOM_UNAVAILABLE", exception.getErrorCode());
    }

    @Test
    void shouldNotCreateReservationWhenCapacityExceeded() {
        ApiException exception = assertThrows(ApiException.class, () -> {
            reservationService.createReservation(
                    "101", "João Silva", "joao@example.com", "12345678900",
                    LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                    3, RatePlan.STANDARD
            );
        });

        assertEquals("CAPACITY_EXCEEDED", exception.getErrorCode());
    }

    @Test
    void shouldConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    void shouldCancelReservationWithRefundForStandard() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    void shouldCancelReservationWithoutRefundForNonRefundable() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(reservation.getId());

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    void shouldDoCheckIn() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());

        Reservation checkedIn = reservationService.checkIn(reservation.getId());

        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    void shouldDoCheckOut() {
        Reservation reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());
        reservationService.checkIn(reservation.getId());

        Reservation checkedOut = reservationService.checkOut(reservation.getId());

        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }
}
