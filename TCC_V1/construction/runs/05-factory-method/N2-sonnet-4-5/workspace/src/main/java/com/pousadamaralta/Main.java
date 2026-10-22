package com.pousadamaralta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamaralta.api.controller.MessageController;
import com.pousadamaralta.api.controller.ReportController;
import com.pousadamaralta.api.controller.ReservationController;
import com.pousadamaralta.api.controller.RoomController;
import com.pousadamaralta.domain.service.MessageService;
import com.pousadamaralta.domain.service.PricingService;
import com.pousadamaralta.domain.service.ReportService;
import com.pousadamaralta.domain.service.ReservationService;
import com.pousadamaralta.domain.service.RoomService;
import com.pousadamaralta.infrastructure.database.DatabaseInitializer;
import com.pousadamaralta.infrastructure.repository.MessageRepository;
import com.pousadamaralta.infrastructure.repository.ReservationRepository;
import com.pousadamaralta.infrastructure.repository.RoomRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Main {

    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL");
        String dbUser = getEnv("DB_USER");
        String dbPassword = getEnv("DB_PASSWORD");
        int port = Integer.parseInt(getEnv("PORT", "7070"));

        DataSource dataSource = createDataSource(dbUrl, dbUser, dbPassword);
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        RoomService roomService = new RoomService(roomRepository);
        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, pricingService, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, roomService
        );
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        app.get("/rooms", roomController::list);
        app.post("/reservations", reservationController::create);
        app.get("/reservations/{id}", reservationController::get);
        app.post("/reservations/{id}/confirmation", reservationController::confirm);
        app.post("/reservations/{id}/cancellation", reservationController::cancel);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::list);
        app.get("/reports/occupancy", reportController::occupancy);
        app.get("/reports/revenue", reportController::revenue);

        app.start(port);

        System.out.println("Pousada Maré Alta reservation system running on port " + port);
    }

    private static DataSource createDataSource(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new IllegalStateException("Environment variable " + name + " is not set");
        }
        return value;
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
