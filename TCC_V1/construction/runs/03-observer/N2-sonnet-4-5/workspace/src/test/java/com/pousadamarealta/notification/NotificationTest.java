package com.pousadamarealta.notification;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.pricing.PricingService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {
    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private MessageRepository messageRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        roomRepository = new RoomRepository();
        reservationRepository = new ReservationRepository();
        messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();

        GuestNotifier guestNotifier = new GuestNotifier(messageRepository);
        HousekeepingNotifier housekeepingNotifier = new HousekeepingNotifier(messageRepository);
        FinanceNotifier financeNotifier = new FinanceNotifier(messageRepository);

        List<ReservationListener> listeners = List.of(
            guestNotifier,
            housekeepingNotifier,
            financeNotifier
        );

        reservationService = new ReservationService(
            roomRepository,
            reservationRepository,
            pricingService,
            listeners
        );
    }

    @Test
    void confirmationSendsThreeEmails() {
        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            201,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        Reservation reservation = reservationService.create(request);
        reservationService.confirm(reservation.id());

        List<Message> messages = messageRepository.findAll();
        assertThat(messages).hasSize(3);

        Message guestEmail = messages.get(0);
        assertThat(guestEmail.to()).isEqualTo("carla@example.com");
        assertThat(guestEmail.subject()).isEqualTo("Reserva confirmada - Pousada Maré Alta");
        assertThat(guestEmail.body()).contains("Olá, Carla Dias!");
        assertThat(guestEmail.body()).contains("Comprovante de reserva");

        Message housekeepingEmail = messages.get(1);
        assertThat(housekeepingEmail.to()).isEqualTo("governanca@pousadamarealta.com.br");
        assertThat(housekeepingEmail.subject()).isEqualTo("Reserva confirmada - quarto 201");
        assertThat(housekeepingEmail.body()).contains("Entrada: 10/03/2027");
        assertThat(housekeepingEmail.body()).contains("Saída: 12/03/2027");
        assertThat(housekeepingEmail.body()).contains("Hóspedes: 2");

        Message financeEmail = messages.get(2);
        assertThat(financeEmail.to()).isEqualTo("financeiro@pousadamarealta.com.br");
        assertThat(financeEmail.subject()).isEqualTo("Pagamento recebido - reserva 1");
        assertThat(financeEmail.body()).contains("Valor: R$ 779,80");
    }

    @Test
    void cancellationOfConfirmedReservationNotifiesHousekeepingAndFinance() {
        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            201,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        Reservation reservation = reservationService.create(request);
        reservationService.confirm(reservation.id());
        int messagesBeforeCancellation = messageRepository.findAll().size();

        reservationService.cancel(reservation.id());

        List<Message> allMessages = messageRepository.findAll();
        List<Message> messages = allMessages.subList(messagesBeforeCancellation, allMessages.size());
        assertThat(messages).hasSize(2);

        Message housekeepingEmail = messages.get(0);
        assertThat(housekeepingEmail.to()).isEqualTo("governanca@pousadamarealta.com.br");
        assertThat(housekeepingEmail.subject()).isEqualTo("Reserva cancelada - quarto 201");

        Message financeEmail = messages.get(1);
        assertThat(financeEmail.to()).isEqualTo("financeiro@pousadamarealta.com.br");
        assertThat(financeEmail.subject()).isEqualTo("Reembolso - reserva 1");
        assertThat(financeEmail.body()).contains("Valor: R$ 779,80");
    }

    @Test
    void cancellationOfPendingReservationSendsNoEmails() {
        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            201,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        Reservation reservation = reservationService.create(request);
        reservationService.cancel(reservation.id());

        List<Message> messages = messageRepository.findAll();
        assertThat(messages).isEmpty();
    }

    @Test
    void cancellationOfNonRefundableDoesNotNotifyFinance() {
        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            201,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );
        Reservation reservation = reservationService.create(request);
        reservationService.confirm(reservation.id());
        int messagesBeforeCancellation = messageRepository.findAll().size();

        reservationService.cancel(reservation.id());

        List<Message> allMessages = messageRepository.findAll();
        List<Message> messages = allMessages.subList(messagesBeforeCancellation, allMessages.size());
        assertThat(messages).hasSize(1);

        Message housekeepingEmail = messages.get(0);
        assertThat(housekeepingEmail.to()).isEqualTo("governanca@pousadamarealta.com.br");
    }
}
