package br.com.pousadamarealta;

import br.com.pousadamarealta.api.*;
import br.com.pousadamarealta.config.DataSourceConfig;
import br.com.pousadamarealta.events.EventPublisher;
import br.com.pousadamarealta.events.listeners.FinanceNotifier;
import br.com.pousadamarealta.events.listeners.GuestNotifier;
import br.com.pousadamarealta.events.listeners.HousekeepingNotifier;
import br.com.pousadamarealta.repository.Database;
import br.com.pousadamarealta.repository.MessageRepository;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;
import br.com.pousadamarealta.service.ReportService;
import br.com.pousadamarealta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

public class Application {

    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL");
        String dbUser = getEnv("DB_USER");
        String dbPassword = getEnv("DB_PASSWORD");
        int port = Integer.parseInt(getEnv("PORT", "7070"));

        DataSource dataSource = DataSourceConfig.create(dbUrl, dbUser, dbPassword);
        Database database = new Database(dataSource);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        EventPublisher eventPublisher = new EventPublisher();
        eventPublisher.subscribe(new GuestNotifier(messageRepository, roomRepository));
        eventPublisher.subscribe(new HousekeepingNotifier(messageRepository));
        eventPublisher.subscribe(new FinanceNotifier(messageRepository));

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, eventPublisher
        );
        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        ErrorHandler.register(app);

        new RoomsController(roomRepository).register(app);
        new ReservationsController(reservationService, roomRepository).register(app);
        new MessagesController(messageRepository).register(app);
        new ReportsController(reportService).register(app);

        app.start(port);
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Environment variable " + name + " is required");
        }
        return value;
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
