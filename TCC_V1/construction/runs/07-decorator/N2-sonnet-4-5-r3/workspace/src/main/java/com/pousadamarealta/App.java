package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.ApiController;
import com.pousadamarealta.infra.DatabaseSetup;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration not provided");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        DataSource dataSource = createDataSource(dbUrl, dbUser, dbPassword);

        DatabaseSetup dbSetup = new DatabaseSetup(dataSource);
        dbSetup.initialize();

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository, testMode, !testMode);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            pricingService,
            messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController controller = new ApiController(
            roomRepository,
            reservationService,
            messageRepository,
            reportService
        );

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", controller::getRooms);
        app.post("/reservations", controller::createReservation);
        app.get("/reservations/{id}", controller::getReservation);
        app.post("/reservations/{id}/confirmation", controller::confirmReservation);
        app.post("/reservations/{id}/cancellation", controller::cancelReservation);
        app.post("/reservations/{id}/check-in", controller::checkInReservation);
        app.post("/reservations/{id}/check-out", controller::checkOutReservation);
        app.get("/reservations/{id}/document", controller::getDocument);
        app.get("/messages", controller::getMessages);
        app.get("/reports/occupancy", controller::getOccupancyReport);
        app.get("/reports/revenue", controller::getRevenueReport);

        app.start(port);
        System.out.println("Server started on port " + port);
    }

    private static DataSource createDataSource(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }
}
