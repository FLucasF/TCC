package br.com.pousadamarealta;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.ReservationStatus;
import br.com.pousadamarealta.events.EventPublisher;
import br.com.pousadamarealta.events.listeners.FinanceNotifier;
import br.com.pousadamarealta.events.listeners.GuestNotifier;
import br.com.pousadamarealta.events.listeners.HousekeepingNotifier;
import br.com.pousadamarealta.repository.Database;
import br.com.pousadamarealta.repository.MessageRepository;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;
import br.com.pousadamarealta.service.ReservationService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationFlowIT {

    private ReservationService reservationService;
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Database database = new Database(dataSource);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        messageRepository = new MessageRepository(database);

        EventPublisher eventPublisher = new EventPublisher();
        eventPublisher.subscribe(new GuestNotifier(messageRepository, roomRepository));
        eventPublisher.subscribe(new HousekeepingNotifier(messageRepository));
        eventPublisher.subscribe(new FinanceNotifier(messageRepository));

        reservationService = new ReservationService(reservationRepository, roomRepository, eventPublisher);
    }

    @Test
    void shouldCreateAndConfirmReservation() {
        Reservation reservation = reservationService.create(
            "101",
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );

        assertNotNull(reservation.getId());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), reservation.getAmountPaid());

        Reservation confirmed = reservationService.confirm(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertTrue(confirmed.getAmountPaid().compareTo(BigDecimal.ZERO) > 0);

        var messages = messageRepository.findAll();
        assertEquals(3, messages.size());

        assertTrue(messages.stream().anyMatch(m ->
            m.getTo().equals("carla@example.com") &&
            m.getSubject().contains("Reserva confirmada")));

        assertTrue(messages.stream().anyMatch(m ->
            m.getTo().equals("governanca@pousadamarealta.com.br") &&
            m.getSubject().contains("Reserva confirmada")));

        assertTrue(messages.stream().anyMatch(m ->
            m.getTo().equals("financeiro@pousadamarealta.com.br") &&
            m.getSubject().contains("Pagamento recebido")));
    }

    @Test
    void shouldCancelConfirmedReservationWithRefund() {
        Reservation reservation = reservationService.create(
            "101",
            "João Silva",
            "joao@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );

        reservationService.confirm(reservation.getId());

        int messagesAfterConfirm = messageRepository.findAll().size();

        Reservation cancelled = reservationService.cancel(reservation.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getRefund().compareTo(BigDecimal.ZERO) > 0);

        var messages = messageRepository.findAll();
        assertEquals(messagesAfterConfirm + 2, messages.size());

        assertTrue(messages.stream().anyMatch(m ->
            m.getTo().equals("governanca@pousadamarealta.com.br") &&
            m.getSubject().contains("Reserva cancelada")));

        assertTrue(messages.stream().anyMatch(m ->
            m.getTo().equals("financeiro@pousadamarealta.com.br") &&
            m.getSubject().contains("Reembolso")));
    }

    @Test
    void shouldCancelNonRefundableWithoutRefundNotification() {
        Reservation reservation = reservationService.create(
            "101",
            "Maria Santos",
            "maria@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "NON_REFUNDABLE"
        );

        reservationService.confirm(reservation.getId());

        int messagesAfterConfirm = messageRepository.findAll().size();

        Reservation cancelled = reservationService.cancel(reservation.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), cancelled.getRefund());

        var messages = messageRepository.findAll();
        assertEquals(messagesAfterConfirm + 1, messages.size());

        assertTrue(messages.stream().anyMatch(m ->
            m.getTo().equals("governanca@pousadamarealta.com.br") &&
            m.getSubject().contains("Reserva cancelada")));

        assertFalse(messages.stream().anyMatch(m ->
            m.getSubject().contains("Reembolso")));
    }

    @Test
    void shouldCancelPendingReservationWithoutNotifications() {
        Reservation reservation = reservationService.create(
            "101",
            "Pedro Costa",
            "pedro@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );

        int messagesBefore = messageRepository.findAll().size();

        Reservation cancelled = reservationService.cancel(reservation.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());

        var messages = messageRepository.findAll();
        assertEquals(messagesBefore, messages.size());
    }

    @Test
    void shouldPreventDoubleBooking() {
        reservationService.create(
            "101",
            "First Guest",
            "first@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.create(
                "101",
                "Second Guest",
                "second@example.com",
                "11122233344",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                2,
                "STANDARD"
            );
        });
    }

    @Test
    void shouldEnforceRoomCapacity() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.create(
                "101",
                "Big Family",
                "family@example.com",
                "11122233344",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                3,
                "STANDARD"
            );
        });
    }
}
