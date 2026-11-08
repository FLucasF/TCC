package com.marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marealta.api.controller.MessageController;
import com.marealta.api.controller.ReportController;
import com.marealta.api.controller.ReservationController;
import com.marealta.api.controller.RoomController;
import com.marealta.api.error.ErrorHandler;
import com.marealta.application.handler.*;
import com.marealta.domain.event.ReservationCancelledEvent;
import com.marealta.domain.event.ReservationConfirmedEvent;
import com.marealta.domain.repository.ReservationRepository;
import com.marealta.domain.repository.RoomRepository;
import com.marealta.domain.service.PricingService;
import com.marealta.domain.service.ReportService;
import com.marealta.domain.service.ReservationService;
import com.marealta.infrastructure.database.DatabaseInitializer;
import com.marealta.infrastructure.database.JdbcMessageStore;
import com.marealta.infrastructure.database.JdbcReservationRepository;
import com.marealta.infrastructure.database.JdbcRoomRepository;
import com.marealta.infrastructure.messaging.EmailService;
import com.marealta.infrastructure.messaging.MessageStore;
import com.marealta.infrastructure.notification.EventDispatcher;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class Main {
    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        DatabaseInitializer dbInitializer = new DatabaseInitializer(dataSource);
        dbInitializer.initialize();

        RoomRepository roomRepository = new JdbcRoomRepository(dataSource);
        ReservationRepository reservationRepository = new JdbcReservationRepository(dataSource);
        MessageStore messageStore = new JdbcMessageStore(dataSource);

        EventDispatcher eventDispatcher = new EventDispatcher();
        EmailService emailService = new EmailService(messageStore);

        registerEventHandlers(eventDispatcher, emailService);

        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            pricingService,
            eventDispatcher
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService,
            reservationRepository
        );
        MessageController messageController = new MessageController(messageStore);
        ReportController reportController = new ReportController(reportService);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.jsonMapper(new JavalinJackson(mapper, false));
        }).start(port);

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

        app.exception(Exception.class, ErrorHandler::handle);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor rodando na porta " + port);
    }

    private static void registerEventHandlers(EventDispatcher dispatcher, EmailService emailService) {
        dispatcher.register(ReservationConfirmedEvent.class, new GuestNotificationHandler(emailService));
        dispatcher.register(ReservationConfirmedEvent.class, new HousekeepingNotificationHandler(emailService));
        dispatcher.register(ReservationConfirmedEvent.class, new FinanceNotificationHandler(emailService));

        dispatcher.register(ReservationCancelledEvent.class, new HousekeepingCancellationHandler(emailService));
        dispatcher.register(ReservationCancelledEvent.class, new FinanceCancellationHandler(emailService));
    }

    private static DataSource createDataSource() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                "Database configuration missing. Set DB_URL, DB_USER, and DB_PASSWORD environment variables."
            );
        }

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(url);
        ds.setUser(user);
        ds.setPassword(password);

        return ds;
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
}
