package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.document.DocumentService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.ApiController;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        HikariDataSource dataSource = createDataSource();

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initialize();
        reservationRepository.initialize();
        messageRepository.initialize();

        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageRepository, documentService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            roomRepository, messageRepository, reservationService, reportService);

        int port = getPort();

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        apiController.registerRoutes(app);

        System.out.println("Server started on port " + port);
    }

    private static HikariDataSource createDataSource() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null) {
            throw new IllegalStateException("DB_URL environment variable is not set");
        }
        if (user == null) {
            throw new IllegalStateException("DB_USER environment variable is not set");
        }
        if (password == null) {
            throw new IllegalStateException("DB_PASSWORD environment variable is not set");
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
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value, using default 7070");
            }
        }
        return 7070;
    }
}
