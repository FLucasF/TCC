package com.pousadamarealta;

import com.pousadamarealta.exception.*;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.service.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    private static ReservationService reservationService;
    private static MessageService messageService;
    private static RoomService roomService;

    @BeforeAll
    static void setup() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        DataSource dataSource = new HikariDataSource(config);

        com.pousadamarealta.database.DatabaseInitializer.initialize(dataSource);

        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomService = new RoomService();
        PricingService pricingService = new PricingService();
        messageService = new MessageService(messageRepository, false);
        reservationService = new ReservationService(reservationRepository, roomService, pricingService, messageService);
    }

    @AfterAll
    static void tearDown() {
    }

    @Test
    @Order(1)
    void shouldGetRooms() {
        List<Room> rooms = roomService.getAllRooms();
        assertEquals(5, rooms.size());
        assertEquals("101", rooms.get(0).getNumber());
        assertEquals(RoomType.STANDARD, rooms.get(0).getType());
        assertEquals(2, rooms.get(0).getCapacity());
        assertEquals(249.90, rooms.get(0).getNightlyRate(), 0.01);
    }

    @Test
    @Order(2)
    void shouldCreateReservation() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertNotNull(reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("0.00"), reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
    }

    @Test
    @Order(3)
    void shouldCalculateHighSeasonPricing() {
        Reservation reservation = reservationService.createReservation(
            "102",
            "Maria Santos",
            "maria@example.com",
            "98765432109",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
    }

    @Test
    @Order(4)
    void shouldApplyNonRefundableDiscount() {
        Reservation reservation = reservationService.createReservation(
            "201",
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    @Order(5)
    void shouldRejectOverCapacity() {
        assertThrows(CapacityExceededException.class, () -> {
            reservationService.createReservation(
                "101",
                "Pedro Costa",
                "pedro@example.com",
                "55566677788",
                LocalDate.of(2027, 4, 1),
                LocalDate.of(2027, 4, 2),
                5,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    @Order(6)
    void shouldRejectConflictingReservation() {
        assertThrows(RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                "101",
                "Ana Lima",
                "ana@example.com",
                "99988877766",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    @Order(7)
    void shouldConfirmReservationAndSendEmail() {
        Reservation reservation = reservationService.createReservation(
            "202",
            "Carlos Mendes",
            "carlos@example.com",
            "11111111111",
            LocalDate.of(2027, 5, 1),
            LocalDate.of(2027, 5, 3),
            2,
            RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());

        List<Message> messages = messageService.getAllMessages();
        assertTrue(messages.size() > 0);

        Message lastMessage = messages.get(messages.size() - 1);
        assertEquals(MessageChannel.EMAIL, lastMessage.getChannel());
        assertEquals("carlos@example.com", lastMessage.getTo());
        assertEquals(MessageStatus.SENT, lastMessage.getStatus());
        assertTrue(lastMessage.getSubject().contains("confirmada"));
    }

    @Test
    @Order(8)
    void shouldHandleFullReservationWorkflow() {
        Reservation reservation = reservationService.createReservation(
            "301",
            "Patricia Alves",
            "patricia@example.com",
            "22222222222",
            LocalDate.of(2027, 6, 10),
            LocalDate.of(2027, 6, 12),
            3,
            RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());

        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    @Order(9)
    void shouldCancelReservationWithRefund() {
        Reservation reservation = reservationService.createReservation(
            "102",
            "Rafael Costa",
            "rafael@example.com",
            "33333333333",
            LocalDate.of(2027, 8, 1),
            LocalDate.of(2027, 8, 3),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    @Order(10)
    void shouldCancelNonRefundableWithoutRefund() {
        Reservation reservation = reservationService.createReservation(
            "201",
            "Sandra Lima",
            "sandra@example.com",
            "44444444444",
            LocalDate.of(2027, 9, 1),
            LocalDate.of(2027, 9, 3),
            2,
            RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }
}
