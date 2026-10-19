package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.event.Event;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {
    private ReservationRepository reservationRepository;
    private PricingService pricingService;
    private EventBus eventBus;
    private ReservationService reservationService;
    private List<Event> publishedEvents;

    @BeforeEach
    void setUp() {
        reservationRepository = new ReservationRepository();
        pricingService = new PricingService();
        eventBus = new EventBus();
        publishedEvents = new ArrayList<>();
        eventBus.subscribe(publishedEvents::add);
        reservationService = new ReservationService(reservationRepository, pricingService, eventBus);
    }

    @Test
    void shouldCreateReservationWithCorrectCalculations() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            checkIn, checkOut, 2, RatePlan.STANDARD
        );

        assertThat(reservation.getId()).isEqualTo(1L);
        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void shouldConfirmReservationAndPublishEvent() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        Reservation confirmed = reservationService.getReservation(reservation.getId()).get();
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualByComparingTo(confirmed.getTotal());

        assertThat(publishedEvents).hasSize(1);
        assertThat(publishedEvents.get(0)).isInstanceOf(ReservationConfirmedEvent.class);
    }

    @Test
    void shouldCancelConfirmedReservationAndPublishEvent() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());
        publishedEvents.clear();

        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.getReservation(reservation.getId()).get();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(cancelled.getAmountPaid());

        assertThat(publishedEvents).hasSize(1);
        ReservationCancelledEvent event = (ReservationCancelledEvent) publishedEvents.get(0);
        assertThat(event.wasConfirmed()).isTrue();
    }

    @Test
    void shouldCancelPendingReservationWithoutRefund() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        publishedEvents.clear();

        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.getReservation(reservation.getId()).get();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(publishedEvents).hasSize(1);
        ReservationCancelledEvent event = (ReservationCancelledEvent) publishedEvents.get(0);
        assertThat(event.wasConfirmed()).isFalse();
    }

    @Test
    void shouldNotRefundNonRefundableReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(reservation.getId());

        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.getReservation(reservation.getId()).get();
        assertThat(cancelled.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldDetectRoomConflict() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation first = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 15), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(first.getId());

        boolean hasConflict = reservationService.hasRoomConflict(
            "101", LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 17)
        );

        assertThat(hasConflict).isTrue();
    }

    @Test
    void shouldNotDetectConflictWithCancelledReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation first = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 15), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(first.getId());
        reservationService.cancelReservation(first.getId());

        boolean hasConflict = reservationService.hasRoomConflict(
            "101", LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 17)
        );

        assertThat(hasConflict).isFalse();
    }

    @Test
    void shouldNotAllowConfirmingNonPendingReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = reservationService.createReservation(
            "101", room, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());

        assertThatThrownBy(() -> reservationService.confirmReservation(reservation.getId()))
            .isInstanceOf(IllegalStateException.class);
    }
}
