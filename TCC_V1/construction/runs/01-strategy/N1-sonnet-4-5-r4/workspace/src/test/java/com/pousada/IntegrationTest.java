package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.ApiController;
import com.pousada.repository.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("pousada_test")
            .withUsername("test")
            .withPassword("test");

    private static Javalin app;
    private static ObjectMapper objectMapper;
    private static final int PORT = 7777;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeAll
    static void setup() throws InterruptedException {
        Database.init(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(PORT);

        ApiController controller = new ApiController();
        controller.registerRoutes(app);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Thread.sleep(500);
    }

    @AfterAll
    static void teardown() {
        if (app != null) {
            app.stop();
        }
        Database.close();
        Unirest.shutDown();
    }

    @Test
    @Order(1)
    void shouldListRooms() throws Exception {
        HttpResponse<String> response = Unirest.get(BASE_URL + "/rooms").asString();
        assertEquals(200, response.getStatus());

        List rooms = objectMapper.readValue(response.getBody(), List.class);
        assertEquals(5, rooms.size());
    }

    @Test
    @Order(2)
    void shouldCreateStandardReservation() throws Exception {
        var body = Map.of(
            "roomNumber", "102",
            "guestName", "João Silva",
            "guestEmail", "joao@example.com",
            "guestCpf", "12345678900",
            "checkIn", "2027-03-10",
            "checkOut", "2027-03-12",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(201, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals(1, reservation.get("id"));
        assertEquals("102", reservation.get("roomNumber"));
        assertEquals(2, reservation.get("nights"));
        assertEquals(779.80, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(0.0, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(779.80, ((Number) reservation.get("total")).doubleValue(), 0.01);
        assertEquals("PENDING", reservation.get("status"));
    }

    @Test
    @Order(3)
    void shouldCreateNonRefundableReservation() throws Exception {
        var body = Map.of(
            "roomNumber", "101",
            "guestName", "Maria Santos",
            "guestEmail", "maria@example.com",
            "guestCpf", "98765432100",
            "checkIn", "2027-07-10",
            "checkOut", "2027-07-12",
            "guests", 1,
            "ratePlan", "NON_REFUNDABLE"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(201, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals(599.76, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(89.96, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(509.80, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    @Order(4)
    void shouldCreateCorporateReservation() throws Exception {
        var body = Map.of(
            "roomNumber", "201",
            "guestName", "Carlos Dias",
            "guestEmail", "carlos@example.com",
            "guestCpf", "11122233344",
            "checkIn", "2027-03-10",
            "checkOut", "2027-03-12",
            "guests", 2,
            "ratePlan", "CORPORATE",
            "companyCode", "TECNOSUL"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(201, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals(779.80, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(93.58, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(686.22, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    @Order(5)
    void shouldCreateWeekendPackageReservation() throws Exception {
        var body = Map.of(
            "roomNumber", "201",
            "guestName", "Ana Costa",
            "guestEmail", "ana@example.com",
            "guestCpf", "55566677788",
            "checkIn", "2027-03-12",
            "checkOut", "2027-03-14",
            "guests", 2,
            "ratePlan", "WEEKEND_PACKAGE"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(201, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals(820.00, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(0.0, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(820.00, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    @Order(6)
    void shouldCreateLongStayReservation() throws Exception {
        var body = Map.of(
            "roomNumber", "301",
            "guestName", "Pedro Alves",
            "guestEmail", "pedro@example.com",
            "guestCpf", "99988877766",
            "checkIn", "2027-03-01",
            "checkOut", "2027-03-08",
            "guests", 3,
            "ratePlan", "LONG_STAY"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(201, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals(4059.30, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(405.93, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(3653.37, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    @Order(7)
    void shouldRejectReservationExceedingCapacity() throws Exception {
        var body = Map.of(
            "roomNumber", "101",
            "guestName", "Teste",
            "guestEmail", "teste@example.com",
            "guestCpf", "12345678900",
            "checkIn", "2027-04-01",
            "checkOut", "2027-04-02",
            "guests", 5,
            "ratePlan", "STANDARD"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(422, response.getStatus());

        Map error = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("CAPACITY_EXCEEDED", error.get("error"));
    }

    @Test
    @Order(8)
    void shouldRejectConflictingReservation() throws Exception {
        var body = Map.of(
            "roomNumber", "102",
            "guestName", "Teste",
            "guestEmail", "teste@example.com",
            "guestCpf", "12345678900",
            "checkIn", "2027-03-10",
            "checkOut", "2027-03-12",
            "guests", 1,
            "ratePlan", "STANDARD"
        );

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations")
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .asString();

        assertEquals(409, response.getStatus());

        Map error = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("ROOM_UNAVAILABLE", error.get("error"));
    }

    @Test
    @Order(9)
    void shouldConfirmReservation() throws Exception {
        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations/1/confirmation")
            .asString();

        assertEquals(200, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("CONFIRMED", reservation.get("status"));
        assertEquals(779.80, ((Number) reservation.get("amountPaid")).doubleValue(), 0.01);
    }

    @Test
    @Order(10)
    void shouldCheckInReservation() throws Exception {
        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations/1/check-in")
            .asString();

        assertEquals(200, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("CHECKED_IN", reservation.get("status"));
    }

    @Test
    @Order(11)
    void shouldCheckOutReservation() throws Exception {
        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations/1/check-out")
            .asString();

        assertEquals(200, response.getStatus());

        Map document = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("Comprovante de reserva", document.get("title"));
        assertEquals(1, document.get("reservationId"));
    }

    @Test
    @Order(12)
    void shouldCancelNonRefundableReservation() throws Exception {
        Unirest.post(BASE_URL + "/reservations/2/confirmation").asString();

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations/2/cancellation")
            .asString();

        assertEquals(200, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("CANCELLED", reservation.get("status"));
        assertEquals(0.0, ((Number) reservation.get("refund")).doubleValue(), 0.01);
    }

    @Test
    @Order(13)
    void shouldCancelWeekendPackageWithHalfRefund() throws Exception {
        Unirest.post(BASE_URL + "/reservations/4/confirmation").asString();

        HttpResponse<String> response = Unirest.post(BASE_URL + "/reservations/4/cancellation")
            .asString();

        assertEquals(200, response.getStatus());

        Map reservation = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("CANCELLED", reservation.get("status"));
        assertEquals(410.00, ((Number) reservation.get("refund")).doubleValue(), 0.01);
    }

    @Test
    @Order(14)
    void shouldGenerateOccupancyReport() throws Exception {
        HttpResponse<String> response = Unirest.get(BASE_URL + "/reports/occupancy?date=2027-03-10")
            .asString();

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaders().getFirst("Content-Type").contains("text/csv"));

        String csv = response.getBody();
        assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        assertTrue(csv.contains("total,"));
    }

    @Test
    @Order(15)
    void shouldGenerateRevenueReport() throws Exception {
        HttpResponse<String> response = Unirest.get(BASE_URL + "/reports/revenue?month=2027-03")
            .asString();

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaders().getFirst("Content-Type").contains("text/csv"));

        String csv = response.getBody();
        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(csv.contains("total,"));
    }

    @Test
    @Order(16)
    void shouldListMessages() throws Exception {
        HttpResponse<String> response = Unirest.get(BASE_URL + "/messages")
            .asString();

        assertEquals(200, response.getStatus());

        List messages = objectMapper.readValue(response.getBody(), List.class);
        assertTrue(messages.size() > 0);

        Map firstMessage = (Map) messages.get(0);
        assertEquals("EMAIL", firstMessage.get("channel"));
        assertTrue(((String) firstMessage.get("subject")).contains("Reserva confirmada"));
    }
}
