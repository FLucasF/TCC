package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.ApiController;
import com.pousada.infrastructure.Database;
import com.pousada.messaging.MessageProcessor;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.MessageService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        MessageProcessor messageProcessor = MessageProcessor.createDefault(testMode);

        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository, messageProcessor);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController controller = new ApiController(
            roomService, reservationService, messageService, reportService
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", controller::getRooms);
        app.post("/reservations", controller::createReservation);
        app.get("/reservations/{id}", controller::getReservation);
        app.post("/reservations/{id}/confirmation", controller::confirmReservation);
        app.post("/reservations/{id}/cancellation", controller::cancelReservation);
        app.post("/reservations/{id}/check-in", controller::checkIn);
        app.post("/reservations/{id}/check-out", controller::checkOut);
        app.get("/reservations/{id}/document", controller::getDocument);
        app.get("/messages", controller::getMessages);
        app.get("/reports/occupancy", controller::getOccupancyReport);
        app.get("/reports/revenue", controller::getRevenueReport);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(new com.pousada.api.ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }
}
