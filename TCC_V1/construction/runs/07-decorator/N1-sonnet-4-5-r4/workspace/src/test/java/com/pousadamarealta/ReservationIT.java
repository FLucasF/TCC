package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import java.sql.Statement;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
public class ReservationIT {
    private static Javalin app;
    private static HttpClient client;
    private static ObjectMapper mapper;
    private static final int TEST_PORT = 7071;

    @BeforeAll
    static void setUp() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        DatabaseManager.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        MessageService messageService = new MessageService(messageRepository, false);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageService);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
                reservationService, roomRepository);
        MessageController messageController = new MessageController(messageService);

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));
        });

        app.get("/rooms", roomController::getAll);
        app.post("/reservations", reservationController::create);
        app.get("/reservations/{id}", reservationController::getById);
        app.post("/reservations/{id}/confirmation", reservationController::confirm);
        app.post("/reservations/{id}/cancellation", reservationController::cancel);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getAll);

        app.start(TEST_PORT);
        client = HttpClient.newHttpClient();
    }

    @BeforeEach
    void cleanDatabase() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE reservations RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE messages RESTART IDENTITY CASCADE");
        }
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
        }
        DatabaseManager.close();
    }

    @Test
    void testFullReservationFlow() throws IOException, InterruptedException {
        String reservationJson = """
            {
                "roomNumber": "301",
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "123.456.789-00",
                "checkIn": "2028-03-10",
                "checkOut": "2028-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reservationJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createResponse.statusCode()).isEqualTo(201);

        Map<String, Object> created = mapper.readValue(createResponse.body(), Map.class);
        assertThat(created.get("status")).isEqualTo("PENDING");
        assertThat(((Number) created.get("amountPaid")).doubleValue()).isEqualTo(0.0);

        int reservationId = (int) created.get("id");

        HttpRequest confirmRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/reservations/" + reservationId + "/confirmation"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> confirmResponse = client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(confirmResponse.statusCode()).isEqualTo(200);

        Map<String, Object> confirmed = mapper.readValue(confirmResponse.body(), Map.class);
        assertThat(confirmed.get("status")).isEqualTo("CONFIRMED");
        assertThat(((Number) confirmed.get("amountPaid")).doubleValue()).isGreaterThan(0);
    }

    @Test
    void testHighSeasonPricing() throws IOException, InterruptedException {
        String reservationJson = """
            {
                "roomNumber": "101",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "987.654.321-00",
                "checkIn": "2028-07-20",
                "checkOut": "2028-07-22",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reservationJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createResponse.statusCode()).isEqualTo(201);

        Map<String, Object> created = mapper.readValue(createResponse.body(), Map.class);

        BigDecimal subtotal = new BigDecimal(created.get("subtotal").toString());
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void testCapacityExceeded() throws IOException, InterruptedException {
        String reservationJson = """
            {
                "roomNumber": "102",
                "guestName": "Carlos Dias",
                "guestEmail": "carlos@example.com",
                "guestCpf": "111.222.333-44",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-12",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reservationJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createResponse.statusCode()).isEqualTo(422);

        Map<String, Object> error = mapper.readValue(createResponse.body(), Map.class);
        assertThat(error.get("error")).isEqualTo("CAPACITY_EXCEEDED");
    }
}
