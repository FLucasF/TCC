package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.ApiHandlers;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.infrastructure.MessageRepository;
import com.pousadamarealta.infrastructure.ReservationRepository;
import com.pousadamarealta.infrastructure.RoomRepository;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = getEnvOrDefault("DB_USER", "postgres");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(getEnvOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        NotificationService notificationService = new NotificationService(messageRepository);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, notificationService);
        ReportService reportService = new ReportService(reservationRepository);

        ApiHandlers handlers = new ApiHandlers(
                roomRepository, reservationService, messageRepository,
                notificationService, reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", handlers::getRooms);
        app.post("/reservations", handlers::createReservation);
        app.get("/reservations/{id}", handlers::getReservation);
        app.post("/reservations/{id}/confirmation", handlers::confirmReservation);
        app.post("/reservations/{id}/cancellation", handlers::cancelReservation);
        app.post("/reservations/{id}/check-in", handlers::checkInReservation);
        app.post("/reservations/{id}/check-out", handlers::checkOutReservation);
        app.get("/reservations/{id}/document", handlers::getDocument);
        app.get("/messages", handlers::getMessages);
        app.get("/reports/occupancy", handlers::getOccupancyReport);
        app.get("/reports/revenue", handlers::getRevenueReport);
        app.get("/reports/tourism-tax", handlers::getTourismTaxReport);
        app.get("/reports/rate-plans", handlers::getRatePlansReport);

        app.start(port);

        System.out.println("Sistema da Pousada Maré Alta iniciado na porta " + port);
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
