package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.application.notification.*;
import com.pousadamarealta.application.service.ReportService;
import com.pousadamarealta.application.service.ReservationService;
import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.infrastructure.http.ReservationController;
import com.pousadamarealta.infrastructure.persistence.DatabaseInitializer;
import com.pousadamarealta.infrastructure.persistence.InMemoryRoomRepository;
import com.pousadamarealta.infrastructure.persistence.PostgresMessageRepository;
import com.pousadamarealta.infrastructure.persistence.PostgresReservationRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Main {
    public static void main(String[] args) {
        DataSource dataSource = createDataSource();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new InMemoryRoomRepository();
        ReservationRepository reservationRepository = new PostgresReservationRepository(dataSource);
        MessageRepository messageRepository = new PostgresMessageRepository(dataSource);

        EventPublisher eventPublisher = new EventPublisher();
        NotificationService notificationService = new NotificationService(messageRepository);

        eventPublisher.subscribe(ReservationConfirmedEvent.class,
            new GuestNotificationListener(notificationService, roomRepository));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
            new HousekeepingNotificationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
            new FinanceNotificationListener(notificationService));
        eventPublisher.subscribe(ReservationCancelledEvent.class,
            new HousekeepingCancellationListener(notificationService));
        eventPublisher.subscribe(ReservationCancelledEvent.class,
            new FinanceCancellationListener(notificationService));

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, eventPublisher);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ReservationController controller = new ReservationController(
            reservationService, reportService, roomRepository, messageRepository);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        controller.registerRoutes(app);

        System.out.println("Pousada Maré Alta system started on port " + port);
    }

    private static DataSource createDataSource() {
        String jdbcUrl = System.getenv("DB_URL");
        String username = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (jdbcUrl == null) {
            throw new IllegalStateException("DB_URL environment variable not set");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portStr = System.getenv("PORT");
        return portStr != null ? Integer.parseInt(portStr) : 7070;
    }
}
