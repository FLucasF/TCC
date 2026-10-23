package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.ApiController;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.infrastructure.DatabaseInitializer;
import com.pousadamarealta.infrastructure.InMemoryRoomRepository;
import com.pousadamarealta.infrastructure.PostgresMessageRepository;
import com.pousadamarealta.infrastructure.PostgresReservationRepository;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Application {

    public static void main(String[] args) {
        int port = getPort();
        boolean isTestMode = isTestMode();

        DataSource dataSource = createDataSource();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new InMemoryRoomRepository();
        ReservationRepository reservationRepository = new PostgresReservationRepository(dataSource);
        MessageRepository messageRepository = new PostgresMessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService(roomRepository);
        MessageService messageService = new MessageService(messageRepository, isTestMode);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, documentService, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            roomRepository, reservationService, messageService, reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        apiController.registerRoutes(app);

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
        System.out.println("Modo de teste: " + (isTestMode ? "ATIVADO" : "DESATIVADO"));
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }

    private static boolean isTestMode() {
        String testModeEnv = System.getenv("HOTEL_TEST_MODE");
        return "true".equalsIgnoreCase(testModeEnv);
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }
}
