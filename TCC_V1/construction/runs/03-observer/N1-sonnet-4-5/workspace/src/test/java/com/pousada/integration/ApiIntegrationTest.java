package com.pousada.integration;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.event.EventPublisher;
import com.pousada.listener.FinanceNotificationListener;
import com.pousada.listener.GuestNotificationListener;
import com.pousada.listener.HousekeepingNotificationListener;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ApiIntegrationTest {
    private ReservationService reservationService;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        roomService = new RoomService(roomRepository);
        DocumentService documentService = new DocumentService();
        NotificationService notificationService = new NotificationService(
            messageRepository, roomService, documentService);

        EventPublisher eventPublisher = new EventPublisher();
        eventPublisher.register(new GuestNotificationListener(notificationService));
        eventPublisher.register(new HousekeepingNotificationListener(notificationService));
        eventPublisher.register(new FinanceNotificationListener(notificationService));

        reservationService = new ReservationService(
            reservationRepository, roomService, eventPublisher);
    }

    @Test
    void shouldCreateAndConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertNotNull(reservation);
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertTrue(confirmed.getAmountPaid().getAmount().doubleValue() > 0);
    }

    @Test
    void shouldCalculateCorrectPriceForHighSeason() {
        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@example.com", "12345678900",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertEquals(599.76, confirmed.getAmountPaid().getAmount().doubleValue(), 0.01);
    }

    @Test
    void shouldApplyDiscountForNonRefundablePlan() {
        Reservation reservation = reservationService.createReservation(
            "201", "Ana Costa", "ana@example.com", "98765432100",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertEquals(662.83, confirmed.getAmountPaid().getAmount().doubleValue(), 0.01);
    }

    @Test
    void shouldCheckInAndCheckOut() {
        Reservation reservation = reservationService.createReservation(
            "301", "Pedro Alves", "pedro@example.com", "11111111111",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            3, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    void shouldNotAllowCheckInWithoutConfirmation() {
        Reservation reservation = reservationService.createReservation(
            "101", "Maria Santos", "maria@example.com", "22222222222",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1, RatePlan.STANDARD
        );

        assertThrows(ReservationService.OperationNotAllowedException.class, () -> {
            reservationService.checkIn(reservation.getId());
        });
    }

    @Test
    void shouldNotAllowDoubleBooking() {
        reservationService.createReservation(
            "101", "Guest 1", "guest1@example.com", "11111111111",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1, RatePlan.STANDARD
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                "101", "Guest 2", "guest2@example.com", "22222222222",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                1, RatePlan.STANDARD
            );
        });
    }

    @Test
    void shouldNotExceedRoomCapacity() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.createReservation(
                "101", "Big Family", "family@example.com", "33333333333",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                5, RatePlan.STANDARD
            );
        });
    }

    @Test
    void shouldNotAllowInvalidDates() {
        assertThrows(ReservationService.InvalidRequestException.class, () -> {
            reservationService.createReservation(
                "101", "Guest", "guest@example.com", "44444444444",
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 10),
                1, RatePlan.STANDARD
            );
        });
    }
}
