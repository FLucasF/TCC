package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.observer.*;
import com.pousadamarealta.service.EmailService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverTest {
    private EmailService emailService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        PricingService pricingService = new PricingService();
        ReservationEventPublisher eventPublisher = new ReservationEventPublisher();
        reservationService = new ReservationService(pricingService, eventPublisher);

        eventPublisher.attach(new GuestNotificationObserver(emailService, reservationService));
        eventPublisher.attach(new HousekeepingNotificationObserver(emailService));
        eventPublisher.attach(new FinanceNotificationObserver(emailService));
    }

    @Test
    void shouldSendThreeEmailsWhenReservationConfirmed() {
        Reservation reservation = reservationService.createReservation(
                "201", "Carla Dias", "carla@example.com", "11122233344",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        assertEquals(3, emailService.getMessages().size());

        assertTrue(emailService.getMessages().stream()
                .anyMatch(m -> m.getTo().equals("carla@example.com") &&
                             m.getSubject().equals("Reserva confirmada - Pousada Maré Alta")));

        assertTrue(emailService.getMessages().stream()
                .anyMatch(m -> m.getTo().equals("governanca@pousadamarealta.com.br") &&
                             m.getSubject().equals("Reserva confirmada - quarto 201")));

        assertTrue(emailService.getMessages().stream()
                .anyMatch(m -> m.getTo().equals("financeiro@pousadamarealta.com.br") &&
                             m.getSubject().contains("Pagamento recebido")));
    }

    @Test
    void shouldSendTwoEmailsWhenConfirmedReservationCancelledWithRefund() {
        Reservation reservation = reservationService.createReservation(
                "201", "Carla Dias", "carla@example.com", "11122233344",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        int messagesAfterConfirmation = emailService.getMessages().size();

        reservationService.cancelReservation(reservation.getId());

        assertEquals(messagesAfterConfirmation + 2, emailService.getMessages().size());

        assertTrue(emailService.getMessages().stream()
                .anyMatch(m -> m.getTo().equals("governanca@pousadamarealta.com.br") &&
                             m.getSubject().equals("Reserva cancelada - quarto 201")));

        assertTrue(emailService.getMessages().stream()
                .anyMatch(m -> m.getTo().equals("financeiro@pousadamarealta.com.br") &&
                             m.getSubject().contains("Reembolso")));
    }

    @Test
    void shouldSendOneEmailWhenConfirmedReservationCancelledWithoutRefund() {
        Reservation reservation = reservationService.createReservation(
                "201", "Carla Dias", "carla@example.com", "11122233344",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        int messagesAfterConfirmation = emailService.getMessages().size();

        reservationService.cancelReservation(reservation.getId());

        assertEquals(messagesAfterConfirmation + 1, emailService.getMessages().size());

        assertTrue(emailService.getMessages().stream()
                .anyMatch(m -> m.getTo().equals("governanca@pousadamarealta.com.br") &&
                             m.getSubject().equals("Reserva cancelada - quarto 201")));
    }

    @Test
    void shouldNotSendEmailsWhenPendingReservationCancelled() {
        Reservation reservation = reservationService.createReservation(
                "201", "Carla Dias", "carla@example.com", "11122233344",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        reservationService.cancelReservation(reservation.getId());

        assertEquals(0, emailService.getMessages().size());
    }
}
