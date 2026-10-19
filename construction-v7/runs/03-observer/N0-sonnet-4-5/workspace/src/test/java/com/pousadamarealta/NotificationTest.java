package com.pousadamarealta;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.NotificationMessage;
import com.pousadamarealta.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {
    private RoomService roomService;
    private NotificationService notificationService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        notificationService = new NotificationService();
        reservationService = new ReservationService(roomService, notificationService);
    }

    @Test
    void testConfirmationSendsThreeEmails() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        List<NotificationMessage> messages = notificationService.getSentMessages();
        assertEquals(3, messages.size());

        NotificationMessage guestEmail = messages.stream()
            .filter(m -> m.getTo().equals("carla@example.com"))
            .findFirst()
            .orElseThrow();
        assertEquals("EMAIL", guestEmail.getChannel());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestEmail.getSubject());
        assertTrue(guestEmail.getBody().contains("Olá, Carla Dias!"));
        assertTrue(guestEmail.getBody().contains("Comprovante de reserva"));

        NotificationMessage housekeepingEmail = messages.stream()
            .filter(m -> m.getTo().equals("governanca@pousadamarealta.com.br"))
            .findFirst()
            .orElseThrow();
        assertEquals("EMAIL", housekeepingEmail.getChannel());
        assertEquals("Reserva confirmada - quarto 201", housekeepingEmail.getSubject());
        assertTrue(housekeepingEmail.getBody().contains("Entrada: 10/03/2027"));
        assertTrue(housekeepingEmail.getBody().contains("Saída: 12/03/2027"));
        assertTrue(housekeepingEmail.getBody().contains("Hóspedes: 2"));

        NotificationMessage financeEmail = messages.stream()
            .filter(m -> m.getTo().equals("financeiro@pousadamarealta.com.br"))
            .findFirst()
            .orElseThrow();
        assertEquals("EMAIL", financeEmail.getChannel());
        assertTrue(financeEmail.getSubject().contains("Pagamento recebido"));
        assertTrue(financeEmail.getBody().contains("R$ 779,80"));
    }

    @Test
    void testCancelledConfirmedReservationSendsTwoEmails() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        int messagesAfterConfirmation = notificationService.getSentMessages().size();

        reservationService.cancelReservation(reservation.getId());

        List<NotificationMessage> allMessages = notificationService.getSentMessages();
        assertEquals(messagesAfterConfirmation + 2, allMessages.size());

        List<NotificationMessage> cancellationMessages = allMessages.stream()
            .skip(messagesAfterConfirmation)
            .toList();

        NotificationMessage housekeepingEmail = cancellationMessages.stream()
            .filter(m -> m.getTo().equals("governanca@pousadamarealta.com.br"))
            .findFirst()
            .orElseThrow();
        assertTrue(housekeepingEmail.getSubject().contains("Reserva cancelada - quarto 201"));
        assertTrue(housekeepingEmail.getBody().contains("Datas liberadas"));

        NotificationMessage financeEmail = cancellationMessages.stream()
            .filter(m -> m.getTo().equals("financeiro@pousadamarealta.com.br"))
            .findFirst()
            .orElseThrow();
        assertTrue(financeEmail.getSubject().contains("Reembolso"));
        assertTrue(financeEmail.getBody().contains("R$ 779,80"));
    }

    @Test
    void testCancelledNonRefundableDoesNotNotifyFinance() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        int messagesAfterConfirmation = notificationService.getSentMessages().size();

        reservationService.cancelReservation(reservation.getId());

        List<NotificationMessage> allMessages = notificationService.getSentMessages();
        assertEquals(messagesAfterConfirmation + 1, allMessages.size());

        List<NotificationMessage> cancellationMessages = allMessages.stream()
            .skip(messagesAfterConfirmation)
            .toList();

        assertEquals(1, cancellationMessages.size());
        assertEquals("governanca@pousadamarealta.com.br", cancellationMessages.get(0).getTo());
    }

    @Test
    void testCancelledPendingReservationSendsNoEmails() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        int messagesBefore = notificationService.getSentMessages().size();

        reservationService.cancelReservation(reservation.getId());

        assertEquals(messagesBefore, notificationService.getSentMessages().size());
    }
}
