package com.pousadamareaalta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(TestAppRunner.class)
class ReservationSystemIT {

    private static final String BASE_URL = "http://localhost:7070";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @Order(1)
    void shouldGetAllRooms() throws Exception {
        HttpResponse<String> response = get("/rooms");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode rooms = mapper.readTree(response.body());
        assertThat(rooms).hasSize(5);
        assertThat(rooms.get(0).get("number").asInt()).isEqualTo(101);
        assertThat(rooms.get(0).get("type").asText()).isEqualTo("STANDARD");
        assertThat(rooms.get(0).get("capacity").asInt()).isEqualTo(2);
        assertThat(rooms.get(0).get("nightlyRate").asDouble()).isEqualTo(249.90);
    }

    @Test
    @Order(2)
    void shouldCreateStandardReservation() throws Exception {
        String request = """
            {
                "roomNumber": 101,
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "123.456.789-00",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = mapper.readTree(response.body());
        assertThat(reservation.get("id").asLong()).isGreaterThan(0);
        assertThat(reservation.get("status").asText()).isEqualTo("PENDING");
        assertThat(reservation.get("nights").asInt()).isEqualTo(2);
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(499.80);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(0.0);
        assertThat(reservation.get("total").asDouble()).isEqualTo(499.80);
        assertThat(reservation.get("amountPaid").asDouble()).isEqualTo(0.0);
    }

    @Test
    @Order(3)
    void shouldCreateNonRefundableReservation() throws Exception {
        String request = """
            {
                "roomNumber": 201,
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "987.654.321-00",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = mapper.readTree(response.body());
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(779.80);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(116.97);
        assertThat(reservation.get("total").asDouble()).isEqualTo(662.83);
    }

    @Test
    @Order(4)
    void shouldCreateCorporateReservation() throws Exception {
        String request = """
            {
                "roomNumber": 201,
                "guestName": "Carlos Dias",
                "guestEmail": "carlos@example.com",
                "guestCpf": "111.222.333-44",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-12",
                "guests": 2,
                "ratePlan": "CORPORATE",
                "companyCode": "TECNOSUL"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = mapper.readTree(response.body());
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(779.80);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(93.58);
        assertThat(reservation.get("total").asDouble()).isEqualTo(686.22);
    }

    @Test
    @Order(5)
    void shouldCreateWeekendPackageReservation() throws Exception {
        String request = """
            {
                "roomNumber": 201,
                "guestName": "Ana Costa",
                "guestEmail": "ana@example.com",
                "guestCpf": "444.555.666-77",
                "checkIn": "2027-03-12",
                "checkOut": "2027-03-14",
                "guests": 2,
                "ratePlan": "WEEKEND_PACKAGE"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = mapper.readTree(response.body());
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(820.00);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(0.0);
        assertThat(reservation.get("total").asDouble()).isEqualTo(820.00);
    }

    @Test
    @Order(6)
    void shouldCreateLongStayReservation() throws Exception {
        String request = """
            {
                "roomNumber": 102,
                "guestName": "Pedro Alves",
                "guestEmail": "pedro@example.com",
                "guestCpf": "555.666.777-88",
                "checkIn": "2027-03-01",
                "checkOut": "2027-03-08",
                "guests": 1,
                "ratePlan": "LONG_STAY"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = mapper.readTree(response.body());
        assertThat(reservation.get("nights").asInt()).isEqualTo(7);
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(1749.30);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(174.93);
        assertThat(reservation.get("total").asDouble()).isEqualTo(1574.37);
    }

    @Test
    @Order(7)
    void shouldApplyHighSeasonPricing() throws Exception {
        String request = """
            {
                "roomNumber": 101,
                "guestName": "Test User",
                "guestEmail": "test@example.com",
                "guestCpf": "999.888.777-66",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode reservation = mapper.readTree(response.body());
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(599.76);
    }

    @Test
    @Order(8)
    void shouldRejectInvalidCorporateCode() throws Exception {
        String request = """
            {
                "roomNumber": 201,
                "guestName": "Invalid Corp",
                "guestEmail": "invalid@example.com",
                "guestCpf": "000.000.000-00",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-12",
                "guests": 2,
                "ratePlan": "CORPORATE",
                "companyCode": "INVALID"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode error = mapper.readTree(response.body());
        assertThat(error.get("error").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    @Order(9)
    void shouldRejectWeekendPackageOnWrongDates() throws Exception {
        String request = """
            {
                "roomNumber": 201,
                "guestName": "Wrong Weekend",
                "guestEmail": "wrong@example.com",
                "guestCpf": "000.000.000-00",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "WEEKEND_PACKAGE"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode error = mapper.readTree(response.body());
        assertThat(error.get("error").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    @Order(10)
    void shouldRejectLongStayUnder7Nights() throws Exception {
        String request = """
            {
                "roomNumber": 102,
                "guestName": "Short Stay",
                "guestEmail": "short@example.com",
                "guestCpf": "000.000.000-00",
                "checkIn": "2027-03-01",
                "checkOut": "2027-03-05",
                "guests": 1,
                "ratePlan": "LONG_STAY"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode error = mapper.readTree(response.body());
        assertThat(error.get("error").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    @Order(11)
    void shouldRejectCapacityExceeded() throws Exception {
        String request = """
            {
                "roomNumber": 101,
                "guestName": "Too Many",
                "guestEmail": "toomany@example.com",
                "guestCpf": "000.000.000-00",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-12",
                "guests": 3,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = post("/reservations", request);

        assertThat(response.statusCode()).isEqualTo(422);
        JsonNode error = mapper.readTree(response.body());
        assertThat(error.get("error").asText()).isEqualTo("CAPACITY_EXCEEDED");
    }

    @Test
    @Order(12)
    void shouldConfirmReservationAndSendEmail() throws Exception {
        HttpResponse<String> confirmResponse = post("/reservations/1/confirmation", "");

        assertThat(confirmResponse.statusCode()).isEqualTo(200);
        JsonNode reservation = mapper.readTree(confirmResponse.body());
        assertThat(reservation.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(reservation.get("amountPaid").asDouble()).isEqualTo(499.80);

        HttpResponse<String> messagesResponse = get("/messages");
        JsonNode messages = mapper.readTree(messagesResponse.body());
        assertThat(messages.size()).isGreaterThan(0);
        assertThat(messages.get(0).get("channel").asText()).isEqualTo("EMAIL");
        assertThat(messages.get(0).get("to").asText()).isEqualTo("joao@example.com");
        assertThat(messages.get(0).get("subject").asText()).isEqualTo("Reserva confirmada - Pousada Maré Alta");
    }

    @Test
    @Order(13)
    void shouldCheckInAndCheckOut() throws Exception {
        HttpResponse<String> checkInResponse = post("/reservations/1/check-in", "");
        assertThat(checkInResponse.statusCode()).isEqualTo(200);
        JsonNode reservation = mapper.readTree(checkInResponse.body());
        assertThat(reservation.get("status").asText()).isEqualTo("CHECKED_IN");

        HttpResponse<String> checkOutResponse = post("/reservations/1/check-out", "");
        assertThat(checkOutResponse.statusCode()).isEqualTo(200);
        JsonNode receipt = mapper.readTree(checkOutResponse.body());
        assertThat(receipt.get("title").asText()).isEqualTo("Comprovante de reserva");
        assertThat(receipt.get("reservationId").asLong()).isEqualTo(1);
    }

    @Test
    @Order(14)
    void shouldCancelWeekendPackageWithPartialRefund() throws Exception {
        HttpResponse<String> confirmResponse = post("/reservations/4/confirmation", "");
        assertThat(confirmResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> cancelResponse = post("/reservations/4/cancellation", "");
        assertThat(cancelResponse.statusCode()).isEqualTo(200);
        JsonNode reservation = mapper.readTree(cancelResponse.body());
        assertThat(reservation.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(reservation.get("refund").asDouble()).isEqualTo(410.00);
    }

    @Test
    @Order(15)
    void shouldGenerateOccupancyReport() throws Exception {
        HttpResponse<String> response = get("/reports/occupancy?date=2027-03-10");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse(""))
            .contains("text/csv");

        String csv = response.body();
        assertThat(csv).contains("room,type,reservation,check_in,check_out");
        assertThat(csv).contains("total,");
    }

    @Test
    @Order(16)
    void shouldGenerateRevenueReport() throws Exception {
        HttpResponse<String> response = get("/reports/revenue?month=2027-03");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse(""))
            .contains("text/csv");

        String csv = response.body();
        assertThat(csv).contains("reservation,room,check_in,check_out,status,amount");
        assertThat(csv).contains("total,");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
