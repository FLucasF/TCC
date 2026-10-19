package com.pousada.domain.service;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.ReservationStatus;
import com.pousada.domain.validation.ValidationException;
import com.pousada.infrastructure.repository.ReservationRepository;
import com.pousada.infrastructure.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        RoomService roomService = new RoomService(roomRepository);
        reservationService = new ReservationService(reservationRepository, roomService);
    }

    @Test
    void shouldCreateReservation() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThat(reservation.getId()).isEqualTo(1L);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo("499.80");
        assertThat(reservation.getDiscount()).isEqualByComparingTo("0.00");
        assertThat(reservation.getTotal()).isEqualByComparingTo("499.80");
    }

    @Test
    void shouldRejectReservationWithCheckOutBeforeCheckIn() {
        assertThatThrownBy(() -> reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 10),
            2, RatePlan.STANDARD
        )).isInstanceOf(ValidationException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_REQUEST");
    }

    @Test
    void shouldRejectReservationExceedingCapacity() {
        assertThatThrownBy(() -> reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            3, RatePlan.STANDARD
        )).isInstanceOf(ValidationException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CAPACITY_EXCEEDED");
    }

    @Test
    void shouldRejectOverlappingReservations() {
        reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            "101", "Maria Santos", "maria@email.com", "98765432100",
            LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13),
            2, RatePlan.STANDARD
        )).isInstanceOf(ValidationException.class)
          .hasFieldOrPropertyWithValue("errorCode", "ROOM_UNAVAILABLE");
    }

    @Test
    void shouldConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualByComparingTo(confirmed.getTotal());
    }

    @Test
    void shouldCancelStandardReservationWithRefund() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(cancelled.getAmountPaid());
    }

    @Test
    void shouldCancelNonRefundableReservationWithoutRefund() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(reservation.getId());

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo("0.00");
    }
}
