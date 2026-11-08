package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.DocumentDto;
import com.pousadamarealta.dto.ReservationDto;
import com.pousadamarealta.dto.RoomDto;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.repository.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTestManual {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    private static Javalin app;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        Database database = new Database(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        RoomRepository roomRepository = new RoomRepository(database.getDataSource());
        ReservationRepository reservationRepository = new ReservationRepository(database.getDataSource());
        MessageRepository messageRepository = new MessageRepository(database.getDataSource());

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, messageService, documentService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", roomController::getRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);
    }

    @Test
    @Order(1)
    void shouldGetRooms() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/rooms");
            assertThat(response.code()).isEqualTo(200);

            RoomDto[] rooms = objectMapper.readValue(response.body().string(), RoomDto[].class);
            assertThat(rooms).hasSize(5);
            assertThat(rooms[0].number()).isEqualTo("101");
            assertThat(rooms[0].capacity()).isEqualTo(2);
            assertThat(rooms[0].nightlyRate()).isEqualByComparingTo("249.90");
        });
    }

    @Test
    @Order(2)
    void shouldCreateReservation() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "101", "João Silva", "joao@example.com", "12345678900",
                "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertThat(response.code()).isEqualTo(201);

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertThat(reservation.id()).isEqualTo(1L);
            assertThat(reservation.roomNumber()).isEqualTo("101");
            assertThat(reservation.guestName()).isEqualTo("João Silva");
            assertThat(reservation.nights()).isEqualTo(2);
            assertThat(reservation.subtotal()).isEqualByComparingTo("499.80");
            assertThat(reservation.discount()).isEqualByComparingTo("0.00");
            assertThat(reservation.total()).isEqualByComparingTo("499.80");
            assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
            assertThat(reservation.amountPaid()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    @Order(3)
    void shouldCalculateHighSeasonPricing() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "102", "Maria Santos", "maria@example.com", "98765432100",
                "2027-07-10", "2027-07-12", 1, RatePlan.STANDARD
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertThat(response.code()).isEqualTo(201);

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertThat(reservation.subtotal()).isEqualByComparingTo("599.76");
        });
    }

    @Test
    @Order(4)
    void shouldApplyNonRefundableDiscount() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "201", "Pedro Costa", "pedro@example.com", "11122233344",
                "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertThat(response.code()).isEqualTo(201);

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertThat(reservation.subtotal()).isEqualByComparingTo("779.80");
            assertThat(reservation.discount()).isEqualByComparingTo("116.97");
            assertThat(reservation.total()).isEqualByComparingTo("662.83");
        });
    }

    @Test
    @Order(5)
    void shouldConfirmReservation() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/reservations/1/confirmation", "");
            assertThat(response.code()).isEqualTo(200);

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(reservation.amountPaid()).isEqualByComparingTo("499.80");
        });
    }

    @Test
    @Order(6)
    void shouldCheckIn() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/reservations/1/check-in", "");
            assertThat(response.code()).isEqualTo(200);

            ReservationDto reservation = objectMapper.readValue(response.body().string(), ReservationDto.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CHECKED_IN);
        });
    }

    @Test
    @Order(7)
    void shouldCheckOut() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/reservations/1/check-out", "");
            assertThat(response.code()).isEqualTo(200);

            DocumentDto document = objectMapper.readValue(response.body().string(), DocumentDto.class);
            assertThat(document.reservationId()).isEqualTo(1L);
            assertThat(document.guestName()).isEqualTo("João Silva");
            assertThat(document.total()).isEqualByComparingTo("499.80");
        });
    }

    @Test
    @Order(8)
    void shouldGenerateOccupancyReport() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/reservations/2/confirmation", "");

            var response = client.get("/reports/occupancy?date=2027-07-10");
            assertThat(response.code()).isEqualTo(200);

            String csv = response.body().string();
            assertThat(csv).contains("Pousada Maré Alta,occupancy,2027-07-10");
            assertThat(csv).contains("room,type,reservation,check_in,check_out");
            assertThat(csv).contains("102,STANDARD,2,2027-07-10,2027-07-12");
        });
    }

    @Test
    @Order(9)
    void shouldGenerateRevenueReport() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/reports/revenue?month=2027-03");
            assertThat(response.code()).isEqualTo(200);

            String csv = response.body().string();
            assertThat(csv).contains("Pousada Maré Alta,revenue,2027-03");
            assertThat(csv).contains("reservation,room,check_in,check_out,status,amount");
            assertThat(csv).contains("1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80");
        });
    }

    @Test
    @Order(10)
    void shouldGenerateTourismTaxReport() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/reports/tourism-tax?month=2027-03");
            assertThat(response.code()).isEqualTo(200);

            String csv = response.body().string();
            assertThat(csv).contains("Pousada Maré Alta,tourism-tax,2027-03");
            assertThat(csv).contains("reservation,room,check_in,check_out,guests,nights,tax");
            assertThat(csv).contains("1,101,2027-03-10,2027-03-12,2,2,18.00");
        });
    }

    @Test
    @Order(11)
    void shouldGenerateRatePlansReport() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/reservations/3/confirmation", "");

            var response = client.get("/reports/rate-plans?month=2027-03");
            assertThat(response.code()).isEqualTo(200);

            String csv = response.body().string();
            assertThat(csv).contains("Pousada Maré Alta,rate-plans,2027-03");
            assertThat(csv).contains("rate_plan,reservations,amount");
            assertThat(csv).contains("STANDARD,1,499.80");
            assertThat(csv).contains("NON_REFUNDABLE,1,662.83");
        });
    }

    @Test
    @Order(12)
    void shouldRejectRoomConflict() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "102", "Ana Paula", "ana@example.com", "55566677788",
                "2027-07-11", "2027-07-13", 1, RatePlan.STANDARD
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertThat(response.code()).isEqualTo(409);
            assertThat(response.body().string()).contains("ROOM_UNAVAILABLE");
        });
    }

    @Test
    @Order(13)
    void shouldRejectCapacityExceeded() {
        JavalinTest.test(app, (server, client) -> {
            CreateReservationRequest request = new CreateReservationRequest(
                "101", "Lucas Mendes", "lucas@example.com", "77788899900",
                "2027-08-10", "2027-08-12", 5, RatePlan.STANDARD
            );

            var response = client.post("/reservations", objectMapper.writeValueAsString(request));
            assertThat(response.code()).isEqualTo(422);
            assertThat(response.body().string()).contains("CAPACITY_EXCEEDED");
        });
    }
}
