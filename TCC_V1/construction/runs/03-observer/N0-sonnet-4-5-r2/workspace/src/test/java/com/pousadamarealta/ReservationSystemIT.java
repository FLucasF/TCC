package com.pousadamarealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReservationSystemIT {

    private static final String BASE_URL = "http://localhost:7070";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Order(1)
    public void testGetRooms() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/rooms"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode rooms = mapper.readTree(response.body());
        assertEquals(5, rooms.size());

        assertEquals("101", rooms.get(0).get("number").asText());
        assertEquals("STANDARD", rooms.get(0).get("type").asText());
        assertEquals(2, rooms.get(0).get("capacity").asInt());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble(), 0.01);
    }

    @Test
    @Order(2)
    public void testCreateReservation() throws IOException, InterruptedException {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        JsonNode reservation = mapper.readTree(response.body());
        assertEquals(1, reservation.get("id").asLong());
        assertEquals("101", reservation.get("roomNumber").asText());
        assertEquals("PENDING", reservation.get("status").asText());
        assertEquals(2, reservation.get("nights").asInt());
        assertEquals(599.76, reservation.get("subtotal").asDouble(), 0.01);
        assertEquals(0.0, reservation.get("discount").asDouble(), 0.01);
        assertEquals(599.76, reservation.get("total").asDouble(), 0.01);
    }

    @Test
    @Order(3)
    public void testCreateReservationWithDiscount() throws IOException, InterruptedException {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "98765432109",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
        """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        JsonNode reservation = mapper.readTree(response.body());
        assertEquals(2, reservation.get("id").asLong());
        assertEquals(779.80, reservation.get("subtotal").asDouble(), 0.01);
        assertEquals(116.97, reservation.get("discount").asDouble(), 0.01);
        assertEquals(662.83, reservation.get("total").asDouble(), 0.01);
    }

    @Test
    @Order(4)
    public void testConfirmReservation() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/1/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode reservation = mapper.readTree(response.body());
        assertEquals("CONFIRMED", reservation.get("status").asText());
        assertEquals(599.76, reservation.get("amountPaid").asDouble(), 0.01);
    }

    @Test
    @Order(5)
    public void testMessagesAfterConfirmation() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/messages"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode messages = mapper.readTree(response.body());
        assertEquals(3, messages.size());

        boolean foundGuest = false;
        boolean foundHousekeeping = false;
        boolean foundFinance = false;

        for (JsonNode msg : messages) {
            String to = msg.get("to").asText();
            String subject = msg.get("subject").asText();

            if (to.equals("joao@example.com") && subject.contains("Reserva confirmada")) {
                foundGuest = true;
                assertTrue(msg.get("body").asText().contains("João Silva"));
                assertTrue(msg.get("body").asText().contains("Comprovante de reserva nº 1"));
            } else if (to.equals("governanca@pousadamarealta.com.br") && subject.contains("quarto 101")) {
                foundHousekeeping = true;
                assertTrue(msg.get("body").asText().contains("2 hóspedes") ||
                          msg.get("body").asText().contains("Hóspedes: 2"));
            } else if (to.equals("financeiro@pousadamarealta.com.br") && subject.contains("Pagamento recebido")) {
                foundFinance = true;
                assertTrue(msg.get("body").asText().contains("R$ 599,76"));
            }
        }

        assertTrue(foundGuest, "Guest notification not found");
        assertTrue(foundHousekeeping, "Housekeeping notification not found");
        assertTrue(foundFinance, "Finance notification not found");
    }

    @Test
    @Order(6)
    public void testRoomUnavailable() throws IOException, InterruptedException {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Pedro Costa",
                "guestEmail": "pedro@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-07-11",
                "checkOut": "2027-07-13",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(409, response.statusCode());

        JsonNode error = mapper.readTree(response.body());
        assertEquals("ROOM_UNAVAILABLE", error.get("error").asText());
    }

    @Test
    @Order(7)
    public void testCapacityExceeded() throws IOException, InterruptedException {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Ana Lima",
                "guestEmail": "ana@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-08-01",
                "checkOut": "2027-08-03",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());

        JsonNode error = mapper.readTree(response.body());
        assertEquals("CAPACITY_EXCEEDED", error.get("error").asText());
    }

    @Test
    @Order(8)
    public void testCheckIn() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/1/check-in"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode reservation = mapper.readTree(response.body());
        assertEquals("CHECKED_IN", reservation.get("status").asText());
    }

    @Test
    @Order(9)
    public void testCheckOut() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/1/check-out"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        JsonNode document = mapper.readTree(response.body());
        assertEquals("Comprovante de reserva", document.get("title").asText());
        assertEquals(1, document.get("reservationId").asLong());
        assertEquals("João Silva", document.get("guestName").asText());
    }

    @Test
    @Order(10)
    public void testCancelReservationWithRefund() throws IOException, InterruptedException {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "Carlos Oliveira",
                "guestEmail": "carlos@example.com",
                "guestCpf": "33333333333",
                "checkIn": "2027-09-01",
                "checkOut": "2027-09-03",
                "guests": 3,
                "ratePlan": "STANDARD"
            }
        """;

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode created = mapper.readTree(createResponse.body());
        long reservationId = created.get("id").asLong();

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> cancelResponse = client.send(cancelRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, cancelResponse.statusCode());

        JsonNode cancelled = mapper.readTree(cancelResponse.body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertTrue(cancelled.get("refund").asDouble() > 0);
    }

    @Test
    @Order(11)
    public void testOccupancyReport() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reports/occupancy?date=2027-07-11"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String csv = response.body();
        assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        assertTrue(csv.contains("101,STANDARD,1,2027-07-10,2027-07-12"));
        assertTrue(csv.contains("total,1/5"));
    }

    @Test
    @Order(12)
    public void testRevenueReport() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reports/revenue?month=2027-07"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String csv = response.body();
        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(csv.contains("1,101,2027-07-10,2027-07-12"));
        assertTrue(csv.contains("total,"));
    }
}
