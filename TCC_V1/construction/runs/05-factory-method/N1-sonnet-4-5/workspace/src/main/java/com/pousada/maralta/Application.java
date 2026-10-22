package com.pousada.maralta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.maralta.controller.ApiController;
import com.pousada.maralta.infrastructure.DatabaseConfig;
import com.pousada.maralta.repository.*;
import com.pousada.maralta.service.ReportService;
import com.pousada.maralta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        DatabaseConfig.initialize();

        ReservationRepository reservationRepository = new JdbcReservationRepository();
        RoomRepository roomRepository = new JdbcRoomRepository();
        MessageRepository messageRepository = new JdbcMessageRepository();

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageRepository
        );

        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            reservationService, reportService, roomRepository, messageRepository
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        apiController.registerRoutes(app);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            DatabaseConfig.shutdown();
        }));
    }
}
