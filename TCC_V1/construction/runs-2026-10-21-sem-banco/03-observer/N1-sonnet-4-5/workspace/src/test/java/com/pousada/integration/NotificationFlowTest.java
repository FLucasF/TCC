package com.pousada.integration;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
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

class NotificationFlowTest {
    private ReservationService reservationService;
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        messageRepository = new MessageRepository();

        RoomService roomService = new RoomService(roomRepository);
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
    void shouldSendThreeEmailsWhenReservationIsConfirmed() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        assertEquals(3, messageRepository.findAll().size());

        assertTrue(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("carla@example.com") &&
                          m.getSubject().contains("Reserva confirmada")));

        assertTrue(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("governanca@pousadamarealta.com.br") &&
                          m.getSubject().contains("quarto 201")));

        assertTrue(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("financeiro@pousadamarealta.com.br") &&
                          m.getSubject().contains("Pagamento recebido")));
    }

    @Test
    void shouldSendTwoEmailsWhenConfirmedReservationIsCancelledWithRefund() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        int messagesAfterConfirmation = messageRepository.findAll().size();

        reservationService.cancelReservation(reservation.getId());
        int messagesAfterCancellation = messageRepository.findAll().size();

        assertEquals(2, messagesAfterCancellation - messagesAfterConfirmation);

        assertTrue(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("governanca@pousadamarealta.com.br") &&
                          m.getSubject().contains("cancelada")));

        assertTrue(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("financeiro@pousadamarealta.com.br") &&
                          m.getSubject().contains("Reembolso")));
    }

    @Test
    void shouldSendOneEmailWhenConfirmedNonRefundableReservationIsCancelled() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        int messagesAfterConfirmation = messageRepository.findAll().size();

        reservationService.cancelReservation(reservation.getId());
        int messagesAfterCancellation = messageRepository.findAll().size();

        assertEquals(1, messagesAfterCancellation - messagesAfterConfirmation);

        assertTrue(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("governanca@pousadamarealta.com.br") &&
                          m.getSubject().contains("cancelada")));

        assertFalse(messageRepository.findAll().stream()
            .anyMatch(m -> m.getTo().equals("financeiro@pousadamarealta.com.br") &&
                          m.getSubject().contains("Reembolso")));
    }

    @Test
    void shouldNotSendEmailsWhenPendingReservationIsCancelled() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.cancelReservation(reservation.getId());

        assertEquals(0, messageRepository.findAll().size());
    }
}
