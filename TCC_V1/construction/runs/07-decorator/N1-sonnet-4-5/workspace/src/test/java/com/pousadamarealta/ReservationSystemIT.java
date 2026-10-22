package com.pousadamarealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSystemIT {
    private static Process serverProcess;
    private static final int TEST_PORT = 7071;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeAll
    static void startServer() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null) {
            dbUrl = "jdbc:postgresql://localhost:5432/pousada_test";
        }
        String dbUser = System.getenv("DB_USER");
        if (dbUser == null) {
            dbUser = "postgres";
        }
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null) {
            dbPassword = "postgres";
        }

        cleanDatabase(dbUrl, dbUser, dbPassword);

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp", "target/classes:" + System.getProperty("java.class.path"),
                "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
                "com.pousadamarealta.App"
        );
        pb.environment().put("PORT", String.valueOf(TEST_PORT));
        pb.environment().put("DB_URL", dbUrl);
        pb.environment().put("DB_USER", dbUser);
        pb.environment().put("DB_PASSWORD", dbPassword);
        pb.environment().put("HOTEL_TEST_MODE", "false");

        serverProcess = pb.start();

        Thread.sleep(3000);
    }

    @AfterAll
    static void stopServer() {
        if (serverProcess != null) {
            serverProcess.destroy();
        }
    }

    private static void cleanDatabase(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(dbUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPassword);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS messages");
            stmt.execute("DROP TABLE IF EXISTS reservations");
            stmt.execute("DROP TABLE IF EXISTS rooms");
        }
    }

    @Test
    @Order(1)
    void testListRooms() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/rooms"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode rooms = objectMapper.readTree(response.body());
        assertThat(rooms).hasSize(5);
        assertThat(rooms.get(0).get("number").asText()).isEqualTo("101");
        assertThat(rooms.get(0).get("type").asText()).isEqualTo("STANDARD");
        assertThat(rooms.get(0).get("capacity").asInt()).isEqualTo(2);
        assertThat(rooms.get(0).get("nightlyRate").asDouble()).isEqualTo(249.90);
    }

    @Test
    @Order(2)
    void testCreateReservation() throws Exception {
        String requestBody = """
                {
                    "roomNumber": "101",
                    "guestName": "João Silva",
                    "guestEmail": "joao@example.com",
                    "guestCpf": "12345678901",
                    "checkIn": "2027-03-10",
                    "checkOut": "2027-03-12",
                    "guests": 2,
                    "ratePlan": "STANDARD"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = objectMapper.readTree(response.body());
        assertThat(reservation.get("id").asLong()).isEqualTo(1);
        assertThat(reservation.get("status").asText()).isEqualTo("PENDING");
        assertThat(reservation.get("nights").asInt()).isEqualTo(2);
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(499.80);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(0.0);
        assertThat(reservation.get("total").asDouble()).isEqualTo(499.80);
    }

    @Test
    @Order(3)
    void testCreateReservationWithHighSeasonAndDiscount() throws Exception {
        String requestBody = """
                {
                    "roomNumber": "102",
                    "guestName": "Maria Santos",
                    "guestEmail": "maria@example.com",
                    "guestCpf": "98765432109",
                    "checkIn": "2027-07-10",
                    "checkOut": "2027-07-12",
                    "guests": 1,
                    "ratePlan": "NON_REFUNDABLE"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = objectMapper.readTree(response.body());
        assertThat(reservation.get("nights").asInt()).isEqualTo(2);
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(599.76);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(89.96);
        assertThat(reservation.get("total").asDouble()).isEqualTo(509.80);
    }

    @Test
    @Order(4)
    void testConfirmReservation() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations/1/confirmation"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode reservation = objectMapper.readTree(response.body());
        assertThat(reservation.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(reservation.get("amountPaid").asDouble()).isEqualTo(499.80);
    }

    @Test
    @Order(5)
    void testMessagesAfterConfirmation() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/messages"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode messages = objectMapper.readTree(response.body());
        assertThat(messages).hasSize(2);

        JsonNode originalMessage = messages.get(0);
        assertThat(originalMessage.get("channel").asText()).isEqualTo("EMAIL");
        assertThat(originalMessage.get("to").asText()).isEqualTo("joao@example.com");
        assertThat(originalMessage.get("subject").asText()).isEqualTo("Reserva confirmada - Pousada Maré Alta");
        assertThat(originalMessage.get("status").asText()).isEqualTo("SENT");
        assertThat(originalMessage.get("body").asText()).contains("João Silva");
        assertThat(originalMessage.get("body").asText()).contains("Comprovante de reserva nº 1");

        JsonNode archiveMessage = messages.get(1);
        assertThat(archiveMessage.get("to").asText()).isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(archiveMessage.get("subject").asText()).isEqualTo("[Cópia] Reserva confirmada - Pousada Maré Alta");
        assertThat(archiveMessage.get("status").asText()).isEqualTo("SENT");
    }

    @Test
    @Order(6)
    void testRoomUnavailable() throws Exception {
        String requestBody = """
                {
                    "roomNumber": "101",
                    "guestName": "Pedro Costa",
                    "guestEmail": "pedro@example.com",
                    "guestCpf": "11122233344",
                    "checkIn": "2027-03-11",
                    "checkOut": "2027-03-13",
                    "guests": 1,
                    "ratePlan": "STANDARD"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(409);
        JsonNode error = objectMapper.readTree(response.body());
        assertThat(error.get("error").asText()).isEqualTo("ROOM_UNAVAILABLE");
    }

    @Test
    @Order(7)
    void testCapacityExceeded() throws Exception {
        String requestBody = """
                {
                    "roomNumber": "201",
                    "guestName": "Ana Paula",
                    "guestEmail": "ana@example.com",
                    "guestCpf": "55566677788",
                    "checkIn": "2027-04-01",
                    "checkOut": "2027-04-03",
                    "guests": 5,
                    "ratePlan": "STANDARD"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(422);
        JsonNode error = objectMapper.readTree(response.body());
        assertThat(error.get("error").asText()).isEqualTo("CAPACITY_EXCEEDED");
    }

    @Test
    @Order(8)
    void testInvalidEmail() throws Exception {
        String requestBody = """
                {
                    "roomNumber": "201",
                    "guestName": "Carlos Souza",
                    "guestEmail": "carlosgmail.com",
                    "guestCpf": "99988877766",
                    "checkIn": "2027-05-01",
                    "checkOut": "2027-05-03",
                    "guests": 2,
                    "ratePlan": "STANDARD"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);

        HttpRequest confirmRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations/3/confirmation"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest messagesRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/messages"))
                .GET()
                .build();

        HttpResponse<String> messagesResponse = httpClient.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode messages = objectMapper.readTree(messagesResponse.body());

        JsonNode blockedMessage = messages.get(messages.size() - 1);
        assertThat(blockedMessage.get("status").asText()).isEqualTo("BLOCKED");
        assertThat(blockedMessage.get("to").asText()).isEqualTo("carlosgmail.com");
    }

    @Test
    @Order(9)
    void testOccupancyReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reports/occupancy?date=2027-03-11"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .contains("text/csv");

        String csv = response.body();
        assertThat(csv).contains("room,type,reservation,check_in,check_out");
        assertThat(csv).contains("101,STANDARD,1,2027-03-10,2027-03-12");
        assertThat(csv).contains("total,1/5");
    }

    @Test
    @Order(10)
    void testRevenueReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reports/revenue?month=2027-03"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .contains("text/csv");

        String csv = response.body();
        assertThat(csv).contains("reservation,room,check_in,check_out,status,amount");
        assertThat(csv).contains("1,101,2027-03-10,2027-03-12,CONFIRMED,499.80");
        assertThat(csv).contains("total,499.80");
    }
}
