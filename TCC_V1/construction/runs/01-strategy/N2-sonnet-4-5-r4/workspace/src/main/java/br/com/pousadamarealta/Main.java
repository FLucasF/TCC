package br.com.pousadamarealta;

import br.com.pousadamarealta.api.ErrorHandler;
import br.com.pousadamarealta.api.controller.*;
import br.com.pousadamarealta.domain.repository.*;
import br.com.pousadamarealta.domain.service.*;
import br.com.pousadamarealta.infrastructure.database.DatabaseConfig;
import br.com.pousadamarealta.infrastructure.database.DatabaseInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DatabaseConfig databaseConfig = new DatabaseConfig(dbUrl, dbUser, dbPassword);

        try (Connection connection = databaseConfig.getConnection()) {
            DatabaseInitializer.initialize(connection);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }

        DocumentService documentService = new DocumentService();

        RoomController roomController = new RoomController(databaseConfig);
        ReservationController reservationController = new ReservationController(databaseConfig, documentService);
        MessageController messageController = new MessageController(databaseConfig);
        ReportController reportController = new ReportController(databaseConfig);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        ErrorHandler.configure(app);

        app.get("/rooms", roomController::getRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            databaseConfig.close();
        }));

        System.out.println("Pousada Maré Alta - Sistema de Reservas iniciado na porta " + port);
    }
}
