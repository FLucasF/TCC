package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.ApiController;
import com.pousadamarealta.controller.dto.*;
import com.pousadamarealta.domain.PriceCalculator;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    static Javalin app;
    static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        Database database = new Database(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );

        RoomRepository roomRepository = new JdbcRoomRepository(database.getDataSource());
        ReservationRepository reservationRepository = new JdbcReservationRepository(database.getDataSource());
        MessageRepository messageRepository = new JdbcMessageRepository(database.getDataSource());

        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new ArchiveCopyRule());

        MessageProcessor messageProcessor = new MessageProcessor(rules);
        MessageService messageService = new MessageService(messageRepository, messageProcessor);

        PriceCalculator priceCalculator = new PriceCalculator();
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService, priceCalculator);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            reservationService, roomRepository, messageService, reportService);

        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        app.get("/rooms", apiController::getRooms);
        app.post("/reservations", apiController::createReservation);
        app.get("/reservations/{id}", apiController::getReservation);
        app.post("/reservations/{id}/confirmation", apiController::confirmReservation);
        app.post("/reservations/{id}/cancellation", apiController::cancelReservation);
        app.post("/reservations/{id}/check-in", apiController::checkIn);
        app.post("/reservations/{id}/check-out", apiController::checkOut);
        app.get("/reservations/{id}/document", apiController::getDocument);
        app.get("/messages", apiController::getMessages);
        app.get("/reports/occupancy", apiController::getOccupancyReport);
        app.get("/reports/revenue", apiController::getRevenueReport);
    }

    @Test
    @Order(1)
    void testGetRooms() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/rooms");
            assertEquals(200, response.code());

            RoomDto[] rooms = objectMapper.readValue(response.body().string(), RoomDto[].class);
            assertEquals(5, rooms.length);

            RoomDto room101 = rooms[0];
            assertEquals("101", room101.number());
            assertEquals("STANDARD", room101.type());
            assertEquals(2, room101.capacity());
            assertEquals(new BigDecimal("249.90"), room101.nightlyRate());
        });
    }

    @Test
    @Order(2)
    void testCreateReservation() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "201", "Carla Dias", "carla@example.com", "11122233344",
                "2027-03-10", "2027-03-12", 2, "STANDARD"
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertEquals(201, response.code());

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertEquals(1L, reservation.id());
            assertEquals("201", reservation.roomNumber());
            assertEquals("Carla Dias", reservation.guestName());
            assertEquals(2, reservation.nights());
            assertEquals(new BigDecimal("779.80"), reservation.subtotal());
            assertEquals(new BigDecimal("0.00"), reservation.discount());
            assertEquals(new BigDecimal("779.80"), reservation.total());
            assertEquals("PENDING", reservation.status());
        });
    }

    @Test
    @Order(3)
    void testConfirmReservation() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/reservations/1/confirmation", "");
            assertEquals(200, response.code());

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertEquals("CONFIRMED", reservation.status());
            assertEquals(new BigDecimal("779.80"), reservation.amountPaid());
        });
    }

    @Test
    @Order(4)
    void testMessagesAfterConfirmation() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/messages");
            assertEquals(200, response.code());

            MessageDto[] messages = objectMapper.readValue(response.body().string(), MessageDto[].class);
            assertEquals(2, messages.length);

            MessageDto original = messages[0];
            assertEquals("EMAIL", original.channel());
            assertEquals("carla@example.com", original.to());
            assertEquals("Reserva confirmada - Pousada Maré Alta", original.subject());
            assertTrue(original.body().contains("Olá, Carla Dias!"));
            assertEquals("SENT", original.status());

            MessageDto copy = messages[1];
            assertEquals("arquivo@pousadamarealta.com.br", copy.to());
            assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", copy.subject());
            assertEquals("SENT", copy.status());
        });
    }

    @Test
    @Order(5)
    void testCheckIn() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/reservations/1/check-in", "");
            assertEquals(200, response.code());

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertEquals("CHECKED_IN", reservation.status());
        });
    }

    @Test
    @Order(6)
    void testCheckOut() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/reservations/1/check-out", "");
            assertEquals(200, response.code());

            DocumentDto document = objectMapper.readValue(response.body().string(), DocumentDto.class);
            assertEquals("Comprovante de reserva", document.title());
            assertEquals(1L, document.reservationId());
            assertEquals("Carla Dias", document.guestName());
            assertEquals("11122233344", document.guestCpf());
            assertEquals("201", document.roomNumber());
            assertEquals("DELUXE", document.roomType());
        });
    }

    @Test
    @Order(7)
    void testGetDocument() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/reservations/1/document");
            assertEquals(200, response.code());

            DocumentDto document = objectMapper.readValue(response.body().string(), DocumentDto.class);
            assertEquals("Comprovante de reserva", document.title());
        });
    }

    @Test
    @Order(8)
    void testHighSeasonPricing() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "101", "João Silva", "joao@example.com", "12345678900",
                "2027-07-10", "2027-07-12", 2, "STANDARD"
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertEquals(201, response.code());

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertEquals(new BigDecimal("599.76"), reservation.subtotal());
        });
    }

    @Test
    @Order(9)
    void testNonRefundableDiscount() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "102", "Maria Santos", "maria@example.com", "98765432100",
                "2027-04-10", "2027-04-12", 2, "NON_REFUNDABLE"
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertEquals(201, response.code());

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertEquals(new BigDecimal("499.80"), reservation.subtotal());
            assertEquals(new BigDecimal("74.97"), reservation.discount());
            assertEquals(new BigDecimal("424.83"), reservation.total());
        });
    }

    @Test
    @Order(10)
    void testRoomUnavailable() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "201", "Pedro Costa", "pedro@example.com", "11111111111",
                "2027-03-11", "2027-03-13", 2, "STANDARD"
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertEquals(409, response.code());

            ErrorResponse error = objectMapper.readValue(response.body().string(), ErrorResponse.class);
            assertEquals("ROOM_UNAVAILABLE", error.error());
        });
    }

    @Test
    @Order(11)
    void testCapacityExceeded() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "301", "Ana Lima", "ana@example.com", "22222222222",
                "2027-05-10", "2027-05-12", 5, "STANDARD"
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertEquals(422, response.code());

            ErrorResponse error = objectMapper.readValue(response.body().string(), ErrorResponse.class);
            assertEquals("CAPACITY_EXCEEDED", error.error());
        });
    }

    @Test
    @Order(12)
    void testInvalidEmail() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "202", "Carlos Mendes", "carlos.gmail.com", "33333333333",
                "2027-06-10", "2027-06-12", 2, "STANDARD"
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertEquals(201, response.code());

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            Long id = reservation.id();

            client.post("/reservations/" + id + "/confirmation", "");

            var messagesResponse = client.get("/messages");
            MessageDto[] messages = objectMapper.readValue(messagesResponse.body().string(), MessageDto[].class);

            MessageDto blockedMessage = messages[messages.length - 1];
            assertEquals("BLOCKED", blockedMessage.status());
            assertEquals("carlos.gmail.com", blockedMessage.to());
        });
    }

    @Test
    @Order(13)
    void testOccupancyReport() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/reports/occupancy?date=2027-03-11");
            assertEquals(200, response.code());
            assertEquals("text/csv; charset=utf-8", response.header("Content-Type"));

            String csv = response.body().string();
            assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
            assertTrue(csv.contains("201,DELUXE,1,2027-03-10,2027-03-12"));
            assertTrue(csv.contains("total,1/5"));
        });
    }

    @Test
    @Order(14)
    void testRevenueReport() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/reports/revenue?month=2027-03");
            assertEquals(200, response.code());
            assertEquals("text/csv; charset=utf-8", response.header("Content-Type"));

            String csv = response.body().string();
            assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
            assertTrue(csv.contains("1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80"));
            assertTrue(csv.contains("total,779.80"));
        });
    }
}
