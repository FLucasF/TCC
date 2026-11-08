package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.service.ReportService;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import com.pousadamarealta.infrastructure.database.DatabaseInitializer;
import com.pousadamarealta.infrastructure.database.MessageRepository;
import com.pousadamarealta.infrastructure.database.ReservationRepository;
import com.pousadamarealta.infrastructure.database.RoomRepository;
import com.pousadamarealta.infrastructure.web.MessageController;
import com.pousadamarealta.infrastructure.web.ReportController;
import com.pousadamarealta.infrastructure.web.ReservationController;
import com.pousadamarealta.infrastructure.web.RoomController;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Application {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository, roomService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService
        );
        ReportService reportService = new ReportService(reservationService, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, roomService, messageService
        );
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
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

        System.out.println("Sistema de reservas da Pousada Maré Alta iniciado na porta " + port);
    }

    private static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new RuntimeException("Variável DB_URL não configurada");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }
}
