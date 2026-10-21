package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.notification.FinanceNotifier;
import com.pousadamarealta.notification.GuestNotifier;
import com.pousadamarealta.notification.HousekeepingNotifier;
import com.pousadamarealta.notification.ReservationListener;
import com.pousadamarealta.pricing.PricingService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiIntegrationTest {
    private Javalin app;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();

        GuestNotifier guestNotifier = new GuestNotifier(messageRepository);
        HousekeepingNotifier housekeepingNotifier = new HousekeepingNotifier(messageRepository);
        FinanceNotifier financeNotifier = new FinanceNotifier(messageRepository);

        List<ReservationListener> listeners = List.of(
            guestNotifier,
            housekeepingNotifier,
            financeNotifier
        );

        ReservationService reservationService = new ReservationService(
            roomRepository,
            reservationRepository,
            pricingService,
            listeners
        );

        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        ApiController apiController = new ApiController(
            roomRepository,
            reservationRepository,
            messageRepository,
            reservationService,
            reportService,
            guestNotifier
        );

        app = apiController.createApp();

        mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Test
    void getRooms() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/rooms");
            assertThat(response.code()).isEqualTo(200);

            JsonNode rooms = mapper.readTree(response.body().string());
            assertThat(rooms).hasSize(5);
            assertThat(rooms.get(0).get("number").asInt()).isEqualTo(101);
            assertThat(rooms.get(0).get("type").asText()).isEqualTo("STANDARD");
            assertThat(rooms.get(0).get("capacity").asInt()).isEqualTo(2);
            assertThat(rooms.get(0).get("nightly_rate").asDouble()).isEqualTo(249.90);
        });
    }

    @Test
    void createAndConfirmReservation() {
        JavalinTest.test(app, (server, client) -> {
            String body = """
                {
                    "room_number": 201,
                    "guest_name": "Carla Dias",
                    "guest_email": "carla@example.com",
                    "guest_cpf": "11122233344",
                    "check_in": "2027-03-10",
                    "check_out": "2027-03-12",
                    "guests": 2,
                    "rate_plan": "STANDARD"
                }
                """;

            var createResponse = client.post("/reservations", body);
            assertThat(createResponse.code()).isEqualTo(201);

            JsonNode reservation = mapper.readTree(createResponse.body().string());
            assertThat(reservation.get("id").asInt()).isEqualTo(1);
            assertThat(reservation.get("status").asText()).isEqualTo("PENDING");
            assertThat(reservation.get("nights").asInt()).isEqualTo(2);
            assertThat(reservation.get("subtotal").asDouble()).isEqualTo(779.80);
            assertThat(reservation.get("total").asDouble()).isEqualTo(779.80);
            assertThat(reservation.get("amount_paid").asDouble()).isEqualTo(0.0);

            var confirmResponse = client.post("/reservations/1/confirmation", "");
            assertThat(confirmResponse.code()).isEqualTo(200);

            JsonNode confirmed = mapper.readTree(confirmResponse.body().string());
            assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
            assertThat(confirmed.get("amount_paid").asDouble()).isEqualTo(779.80);

            var messagesResponse = client.get("/messages");
            JsonNode messages = mapper.readTree(messagesResponse.body().string());
            assertThat(messages).hasSize(3);
        });
    }

    @Test
    void roomNotFound() {
        JavalinTest.test(app, (server, client) -> {
            String body = """
                {
                    "room_number": 999,
                    "guest_name": "Carla Dias",
                    "guest_email": "carla@example.com",
                    "guest_cpf": "11122233344",
                    "check_in": "2027-03-10",
                    "check_out": "2027-03-12",
                    "guests": 2,
                    "rate_plan": "STANDARD"
                }
                """;

            var response = client.post("/reservations", body);
            assertThat(response.code()).isEqualTo(404);

            JsonNode error = mapper.readTree(response.body().string());
            assertThat(error.get("error").asText()).isEqualTo("ROOM_NOT_FOUND");
        });
    }

    @Test
    void capacityExceeded() {
        JavalinTest.test(app, (server, client) -> {
            String body = """
                {
                    "room_number": 101,
                    "guest_name": "Carla Dias",
                    "guest_email": "carla@example.com",
                    "guest_cpf": "11122233344",
                    "check_in": "2027-03-10",
                    "check_out": "2027-03-12",
                    "guests": 5,
                    "rate_plan": "STANDARD"
                }
                """;

            var response = client.post("/reservations", body);
            assertThat(response.code()).isEqualTo(422);

            JsonNode error = mapper.readTree(response.body().string());
            assertThat(error.get("error").asText()).isEqualTo("CAPACITY_EXCEEDED");
        });
    }

    @Test
    void roomUnavailable() {
        JavalinTest.test(app, (server, client) -> {
            String body1 = """
                {
                    "room_number": 201,
                    "guest_name": "Carla Dias",
                    "guest_email": "carla@example.com",
                    "guest_cpf": "11122233344",
                    "check_in": "2027-03-10",
                    "check_out": "2027-03-12",
                    "guests": 2,
                    "rate_plan": "STANDARD"
                }
                """;
            client.post("/reservations", body1);
            client.post("/reservations/1/confirmation", "");

            String body2 = """
                {
                    "room_number": 201,
                    "guest_name": "João Silva",
                    "guest_email": "joao@example.com",
                    "guest_cpf": "99988877766",
                    "check_in": "2027-03-11",
                    "check_out": "2027-03-13",
                    "guests": 2,
                    "rate_plan": "STANDARD"
                }
                """;

            var response = client.post("/reservations", body2);
            assertThat(response.code()).isEqualTo(409);

            JsonNode error = mapper.readTree(response.body().string());
            assertThat(error.get("error").asText()).isEqualTo("ROOM_UNAVAILABLE");
        });
    }

    @Test
    void occupancyReport() {
        JavalinTest.test(app, (server, client) -> {
            String body = """
                {
                    "room_number": 201,
                    "guest_name": "Carla Dias",
                    "guest_email": "carla@example.com",
                    "guest_cpf": "11122233344",
                    "check_in": "2027-03-10",
                    "check_out": "2027-03-12",
                    "guests": 2,
                    "rate_plan": "STANDARD"
                }
                """;
            client.post("/reservations", body);
            client.post("/reservations/1/confirmation", "");

            var response = client.get("/reports/occupancy?date=2027-03-10");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header("Content-Type")).contains("text/csv");

            String csv = response.body().string();
            assertThat(csv).contains("room,type,reservation,check_in,check_out");
            assertThat(csv).contains("201,DELUXE,1,2027-03-10,2027-03-12");
            assertThat(csv).contains("total,1/5");
        });
    }

    @Test
    void revenueReport() {
        JavalinTest.test(app, (server, client) -> {
            String body = """
                {
                    "room_number": 201,
                    "guest_name": "Carla Dias",
                    "guest_email": "carla@example.com",
                    "guest_cpf": "11122233344",
                    "check_in": "2027-03-10",
                    "check_out": "2027-03-12",
                    "guests": 2,
                    "rate_plan": "STANDARD"
                }
                """;
            client.post("/reservations", body);
            client.post("/reservations/1/confirmation", "");

            var response = client.get("/reports/revenue?month=2027-03");
            assertThat(response.code()).isEqualTo(200);

            String csv = response.body().string();
            assertThat(csv).contains("reservation,room,check_in,check_out,status,amount");
            assertThat(csv).contains("1,201,2027-03-10,2027-03-12,CONFIRMED,779.8");
            assertThat(csv).contains("total,779.8");
        });
    }
}
