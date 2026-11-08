package com.pousadamarealta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousadamarealta.api.ApiException;
import com.pousadamarealta.api.controller.MessageController;
import com.pousadamarealta.api.controller.ReportController;
import com.pousadamarealta.api.controller.ReservationController;
import com.pousadamarealta.api.controller.RoomController;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.notification.FinanceNotifier;
import com.pousadamarealta.notification.GuestNotifier;
import com.pousadamarealta.notification.HousekeepingNotifier;
import com.pousadamarealta.notification.NotificationService;
import com.pousadamarealta.notification.ReservationObserver;
import com.pousadamarealta.repository.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.util.List;

public class App {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        DatabaseInitializer dbInitializer = new DatabaseInitializer(dataSource);
        dbInitializer.initialize();

        RoomRepository roomRepository = new RoomRepository(dataSource);
        roomRepository.initializeRooms();

        MessageRepository messageRepository = new MessageRepository(dataSource);
        NotificationService notificationService = new NotificationService(messageRepository);

        List<ReservationObserver> observers = List.of(
            new GuestNotifier(notificationService),
            new HousekeepingNotifier(notificationService),
            new FinanceNotifier(notificationService)
        );

        ReservationRepository reservationRepository = new ReservationRepository(dataSource, observers);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            roomRepository, reservationRepository, observers
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(roomRepository, reservationRepository);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(createJsonMapper());
        });

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode())
               .json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500)
               .json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        });

        app.get("/rooms", roomController::getRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor rodando na porta " + port);
    }

    private static DataSource createDataSource() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null) {
            url = "jdbc:postgresql://localhost:5432/pousada";
        }
        if (user == null) {
            user = "postgres";
        }
        if (password == null) {
            password = "postgres";
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value, using default 7070");
            }
        }
        return 7070;
    }

    private static JsonMapper createJsonMapper() {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        return new JsonMapper() {
            @Override
            public <T> T fromJsonString(String json, Type targetType) {
                return gson.fromJson(json, targetType);
            }

            @Override
            public String toJsonString(Object obj, Type type) {
                return gson.toJson(obj, type);
            }
        };
    }
}
