package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.events.EventPublisher;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {
    private ReservationService reservationService;
    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private MessageRepository messageRepository;
    private EventPublisher eventPublisher;
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        roomRepository = new RoomRepository();
        reservationRepository = new ReservationRepository();
        messageRepository = new MessageRepository();
        eventPublisher = new EventPublisher();
        receiptService = new ReceiptService(roomRepository);
        reservationService = new ReservationService(
            roomRepository, reservationRepository, messageRepository, eventPublisher, receiptService
        );
    }

    @Test
    void testCreateReservation() {
        Reservation reservation = reservationService.createReservation(
            101, "João Silva", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertThat(reservation.getId()).isEqualTo(1);
        assertThat(reservation.getRoomNumber()).isEqualTo(101);
        assertThat(reservation.getGuests()).isEqualTo(2);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualTo(new BigDecimal("599.76"));
        assertThat(reservation.getDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualTo(new BigDecimal("599.76"));
    }

    @Test
    void testCreateReservationWithDiscount() {
        Reservation reservation = reservationService.createReservation(
            201, "Maria Santos", "maria@email.com", "98765432101",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        assertThat(reservation.getDiscount()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void testCreateReservationRoomNotFound() {
        assertThatThrownBy(() -> reservationService.createReservation(
            999, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        )).hasMessage("ROOM_NOT_FOUND");
    }

    @Test
    void testCreateReservationCapacityExceeded() {
        assertThatThrownBy(() -> reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            3, RatePlan.STANDARD
        )).hasMessage("CAPACITY_EXCEEDED");
    }

    @Test
    void testCreateReservationInvalidDates() {
        assertThatThrownBy(() -> reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 12), LocalDate.of(2027, 7, 10),
            2, RatePlan.STANDARD
        )).hasMessage("INVALID_REQUEST");
    }

    @Test
    void testCreateReservationRoomUnavailable() {
        reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            101, "Maria", "maria@email.com", "98765432101",
            LocalDate.of(2027, 7, 11), LocalDate.of(2027, 7, 13),
            2, RatePlan.STANDARD
        )).hasMessage("ROOM_UNAVAILABLE");
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(
            reservation.getId(), new BigDecimal("599.76")
        );

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualTo(new BigDecimal("599.76"));
        assertThat(messageRepository.findAll().iterator().hasNext()).isTrue();
    }

    @Test
    void testConfirmReservationNotPending() {
        Reservation reservation = reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId(), new BigDecimal("599.76"));

        assertThatThrownBy(() -> reservationService.confirmReservation(
            reservation.getId(), new BigDecimal("599.76")
        )).hasMessage("OPERATION_NOT_ALLOWED");
    }

    @Test
    void testCancelReservationRefund() {
        Reservation reservation = reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId(), new BigDecimal("599.76"));
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualTo(new BigDecimal("599.76"));
    }

    @Test
    void testCancelReservationNoRefund() {
        Reservation reservation = reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId(), new BigDecimal("509.80"));
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void testCheckInCheckOut() {
        Reservation reservation = reservationService.createReservation(
            101, "João", "joao@email.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId(), new BigDecimal("599.76"));
        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertThat(checkedIn.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);

        Reservation checkedOut = reservationService.checkOut(reservation.getId());
        assertThat(checkedOut.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }
}
