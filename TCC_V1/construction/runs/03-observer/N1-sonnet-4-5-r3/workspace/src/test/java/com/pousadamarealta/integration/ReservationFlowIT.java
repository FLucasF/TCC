package com.pousadamarealta.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.domain.events.EventPublisher;
import com.pousadamarealta.domain.events.ReservationCancelled;
import com.pousadamarealta.domain.events.ReservationConfirmed;
import com.pousadamarealta.infrastructure.DatabaseInitializer;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.listeners.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationFlowIT {
    private static DataSource dataSource;
    private static ReservationService reservationService;
    private static MessageRepository messageRepository;

    @BeforeAll
    static void setup() {
        dataSource = createTestDataSource();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new JdbcRoomRepository(dataSource);
        ReservationRepository reservationRepository = new JdbcReservationRepository(dataSource);
        messageRepository = new JdbcMessageRepository(dataSource);

        EventPublisher eventPublisher = new EventPublisher();
        NotificationService notificationService = new NotificationService(messageRepository);

        eventPublisher.subscribe(ReservationConfirmed.class, new GuestConfirmationListener(notificationService, roomRepository));
        eventPublisher.subscribe(ReservationConfirmed.class, new HousekeepingConfirmationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmed.class, new FinanceConfirmationListener(notificationService));

        eventPublisher.subscribe(ReservationCancelled.class, new HousekeepingCancellationListener(notificationService));
        eventPublisher.subscribe(ReservationCancelled.class, new FinanceCancellationListener(notificationService));

        reservationService = new ReservationService(reservationRepository, roomRepository, eventPublisher);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    @Test
    void shouldCreateAndConfirmReservationWithNotifications() {
        var reservation = reservationService.createReservation(
            "201",
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getTotal()).isEqualByComparingTo("779.80");
        assertThat(reservation.getNights()).isEqualTo(2);

        int messagesBeforeConfirmation = messageRepository.findAll().size();

        reservation = reservationService.confirmReservation(reservation.getId());

        assertThat(reservation.getStatus().toString()).isEqualTo("CONFIRMED");
        assertThat(reservation.getAmountPaid()).isEqualByComparingTo("779.80");

        var messages = messageRepository.findAll();
        assertThat(messages).hasSize(messagesBeforeConfirmation + 3);

        assertThat(messages).anyMatch(msg ->
            msg.getTo().equals("carla@example.com") &&
            msg.getSubject().contains("Reserva confirmada")
        );

        assertThat(messages).anyMatch(msg ->
            msg.getTo().equals("governanca@pousadamarealta.com.br") &&
            msg.getSubject().contains("quarto 201")
        );

        assertThat(messages).anyMatch(msg ->
            msg.getTo().equals("financeiro@pousadamarealta.com.br") &&
            msg.getSubject().contains("Pagamento recebido")
        );
    }

    @Test
    void shouldCancelConfirmedReservationWithRefund() {
        var reservation = reservationService.createReservation(
            "102",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 5, 10),
            LocalDate.of(2027, 5, 12),
            2,
            "STANDARD"
        );

        reservationService.confirmReservation(reservation.getId());

        int messagesBeforeCancel = messageRepository.findAll().size();

        reservation = reservationService.cancelReservation(reservation.getId());

        assertThat(reservation.getStatus().toString()).isEqualTo("CANCELLED");
        assertThat(reservation.getRefund()).isEqualByComparingTo(reservation.getTotal());

        var messages = messageRepository.findAll();
        assertThat(messages).hasSize(messagesBeforeCancel + 2);

        assertThat(messages).anyMatch(msg ->
            msg.getTo().equals("governanca@pousadamarealta.com.br") &&
            msg.getSubject().contains("Reserva cancelada")
        );

        assertThat(messages).anyMatch(msg ->
            msg.getTo().equals("financeiro@pousadamarealta.com.br") &&
            msg.getSubject().contains("Reembolso")
        );
    }

    @Test
    void shouldCancelNonRefundableWithoutRefund() {
        var reservation = reservationService.createReservation(
            "301",
            "Maria Santos",
            "maria@example.com",
            "98765432109",
            LocalDate.of(2027, 6, 15),
            LocalDate.of(2027, 6, 17),
            3,
            "NON_REFUNDABLE"
        );

        reservationService.confirmReservation(reservation.getId());

        int messagesBeforeCancel = messageRepository.findAll().size();

        reservation = reservationService.cancelReservation(reservation.getId());

        assertThat(reservation.getStatus().toString()).isEqualTo("CANCELLED");
        assertThat(reservation.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);

        var messages = messageRepository.findAll();
        assertThat(messages).hasSize(messagesBeforeCancel + 1);

        assertThat(messages).anyMatch(msg ->
            msg.getTo().equals("governanca@pousadamarealta.com.br") &&
            msg.getSubject().contains("Reserva cancelada")
        );

        assertThat(messages).noneMatch(msg ->
            msg.getSubject().contains("Reembolso")
        );
    }

    @Test
    void shouldNotSendNotificationsWhenCancellingPendingReservation() {
        var reservation = reservationService.createReservation(
            "101",
            "Pedro Costa",
            "pedro@example.com",
            "11111111111",
            LocalDate.of(2027, 8, 10),
            LocalDate.of(2027, 8, 12),
            1,
            "STANDARD"
        );

        int messagesBeforeCancel = messageRepository.findAll().size();

        reservation = reservationService.cancelReservation(reservation.getId());

        assertThat(reservation.getStatus().toString()).isEqualTo("CANCELLED");

        var messages = messageRepository.findAll();
        assertThat(messages).hasSize(messagesBeforeCancel);
    }

    private static DataSource createTestDataSource() {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada_test");
        String user = System.getenv().getOrDefault("DB_USER", "postgres");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }
}
