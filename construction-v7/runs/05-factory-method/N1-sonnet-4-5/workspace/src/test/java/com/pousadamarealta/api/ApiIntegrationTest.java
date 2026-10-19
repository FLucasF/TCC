package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.controller.MessageController;
import com.pousadamarealta.api.controller.ReportController;
import com.pousadamarealta.api.controller.ReservationController;
import com.pousadamarealta.api.controller.RoomController;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiIntegrationTest {

    private Javalin createApp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageRepository
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService, reservationRepository, roomRepository
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        ExceptionHandler.register(app);

        app.get("/rooms", roomController::listRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::listMessages);
        app.get("/reports/occupancy", reportController::occupancyReport);
        app.get("/reports/revenue", reportController::revenueReport);

        return app;
    }

    @Test
    void testListRooms() {
        JavalinTest.test(createApp(), (server, client) -> {
            var response = client.get("/rooms");
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("101"));
        });
    }

    @Test
    void testCreateAndConfirmReservation() {
        JavalinTest.test(createApp(), (server, client) -> {
            String requestBody = """
                {
                    "roomNumber": "101",
                    "guestName": "Test Guest",
                    "guestEmail": "test@example.com",
                    "guestCpf": "111.222.333-44",
                    "checkIn": "2027-03-10",
                    "checkOut": "2027-03-12",
                    "guests": 2,
                    "ratePlan": "STANDARD"
                }
                """;

            var createResponse = client.post("/reservations", requestBody);
            assertEquals(201, createResponse.code());

            var confirmResponse = client.post("/reservations/1/confirmation", "");
            assertEquals(200, confirmResponse.code());
            assertTrue(confirmResponse.body().string().contains("CONFIRMED"));
        });
    }

    @Test
    void testInvalidIdentification() {
        JavalinTest.test(createApp(), (server, client) -> {
            String requestBody = """
                {
                    "roomNumber": "101",
                    "guestName": "Test Guest",
                    "guestEmail": "test@example.com",
                    "checkIn": "2027-03-10",
                    "checkOut": "2027-03-12",
                    "guests": 2,
                    "ratePlan": "STANDARD"
                }
                """;

            var response = client.post("/reservations", requestBody);
            assertEquals(400, response.code());
            assertTrue(response.body().string().contains("INVALID_REQUEST"));
        });
    }

    @Test
    void testRoomNotFound() {
        JavalinTest.test(createApp(), (server, client) -> {
            String requestBody = """
                {
                    "roomNumber": "999",
                    "guestName": "Test Guest",
                    "guestEmail": "test@example.com",
                    "guestCpf": "111.222.333-44",
                    "checkIn": "2027-03-10",
                    "checkOut": "2027-03-12",
                    "guests": 2,
                    "ratePlan": "STANDARD"
                }
                """;

            var response = client.post("/reservations", requestBody);
            assertEquals(404, response.code());
            assertTrue(response.body().string().contains("ROOM_NOT_FOUND"));
        });
    }

    @Test
    void testCapacityExceeded() {
        JavalinTest.test(createApp(), (server, client) -> {
            String requestBody = """
                {
                    "roomNumber": "101",
                    "guestName": "Test Guest",
                    "guestEmail": "test@example.com",
                    "guestCpf": "111.222.333-44",
                    "checkIn": "2027-03-10",
                    "checkOut": "2027-03-12",
                    "guests": 5,
                    "ratePlan": "STANDARD"
                }
                """;

            var response = client.post("/reservations", requestBody);
            assertEquals(422, response.code());
            assertTrue(response.body().string().contains("CAPACITY_EXCEEDED"));
        });
    }
}
