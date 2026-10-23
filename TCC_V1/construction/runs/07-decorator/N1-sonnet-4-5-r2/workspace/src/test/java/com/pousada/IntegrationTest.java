package com.pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pousada.infrastructure.Database;
import com.pousada.messaging.MessageProcessor;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.MessageService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    private Database database;
    private RoomService roomService;
    private ReservationService reservationService;
    private MessageService messageService;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        database = new Database(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        MessageProcessor messageProcessor = MessageProcessor.createDefault(false);

        roomService = new RoomService(roomRepository);
        messageService = new MessageService(messageRepository, messageProcessor);
        reservationService = new ReservationService(reservationRepository, roomRepository, messageService);
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void shouldCreateAndConfirmReservation() {
        var reservation = reservationService.createReservation(
            "101", "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, "STANDARD"
        );

        assertNotNull(reservation.getId());
        assertEquals("PENDING", reservation.getStatus().name());
        assertEquals(0, reservation.getAmountPaid().compareTo(BigDecimal.ZERO));

        var confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals("CONFIRMED", confirmed.getStatus().name());
        assertTrue(confirmed.getAmountPaid().compareTo(BigDecimal.ZERO) > 0);

        var messages = messageService.getAllMessages();
        assertTrue(messages.size() >= 2);
        assertEquals("SENT", messages.get(0).getStatus().name());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).getTo());
    }

    @Test
    void shouldCalculateHighSeasonPrice() {
        var reservation = reservationService.createReservation(
            "101", "João Santos", "joao@example.com", "98765432100",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            1, "STANDARD"
        );

        var confirmed = reservationService.confirmReservation(reservation.getId());

        BigDecimal expectedNightPrice = new BigDecimal("299.88");
        BigDecimal expectedSubtotal = new BigDecimal("599.76");

        assertEquals(0, confirmed.getSubtotal().compareTo(expectedSubtotal));
        assertEquals(0, confirmed.getTotal().compareTo(expectedSubtotal));
    }

    @Test
    void shouldApplyNonRefundableDiscount() {
        var reservation = reservationService.createReservation(
            "101", "Maria Costa", "maria@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, "NON_REFUNDABLE"
        );

        var confirmed = reservationService.confirmReservation(reservation.getId());

        BigDecimal subtotal = confirmed.getSubtotal();
        BigDecimal expectedDiscount = new BigDecimal("74.98");
        BigDecimal expectedTotal = new BigDecimal("424.87");

        assertEquals(0, confirmed.getDiscount().compareTo(expectedDiscount));
        assertEquals(0, confirmed.getTotal().compareTo(expectedTotal));
    }

    @Test
    void shouldBlockInvalidEmail() {
        var reservation = reservationService.createReservation(
            "102", "Pedro Lima", "pedro.gmail.com", "55566677788",
            LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 2),
            1, "STANDARD"
        );

        reservationService.confirmReservation(reservation.getId());

        var messages = messageService.getAllMessages();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).getStatus().name());
    }

    @Test
    void shouldRedirectInTestMode() {
        MessageProcessor testProcessor = MessageProcessor.createDefault(true);
        MessageService testMessageService = new MessageService(
            new MessageRepository(database), testProcessor
        );
        ReservationService testReservationService = new ReservationService(
            new ReservationRepository(database), new RoomRepository(database), testMessageService
        );

        var reservation = testReservationService.createReservation(
            "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 2),
            2, "STANDARD"
        );

        testReservationService.confirmReservation(reservation.getId());

        var messages = testMessageService.getAllMessages();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).getTo());
        assertTrue(messages.get(0).getSubject().startsWith("[TESTE] "));
        assertTrue(messages.get(0).getBody().contains("Destinatário original: carla@example.com"));
    }

    @Test
    void shouldPreventDoubleBooking() {
        reservationService.createReservation(
            "101", "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 12),
            2, "STANDARD"
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                "101", "João Santos", "joao@example.com", "98765432100",
                LocalDate.of(2027, 6, 11), LocalDate.of(2027, 6, 13),
                1, "STANDARD"
            );
        });
    }

    @Test
    void shouldAllowBookingAfterCancellation() {
        var reservation = reservationService.createReservation(
            "101", "Ana Silva", "ana@example.com", "12345678901",
            LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 12),
            2, "STANDARD"
        );

        reservationService.cancelReservation(reservation.getId());

        assertDoesNotThrow(() -> {
            reservationService.createReservation(
                "101", "João Santos", "joao@example.com", "98765432100",
                LocalDate.of(2027, 6, 11), LocalDate.of(2027, 6, 13),
                1, "STANDARD"
            );
        });
    }

    @Test
    void shouldEnforceCapacity() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.createReservation(
                "101", "Grande Família", "familia@example.com", "11111111111",
                LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 2),
                5, "STANDARD"
            );
        });
    }
}
