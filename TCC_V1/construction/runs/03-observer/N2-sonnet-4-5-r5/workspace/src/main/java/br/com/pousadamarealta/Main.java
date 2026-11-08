package br.com.pousadamarealta;

import br.com.pousadamarealta.api.ApiHandler;
import br.com.pousadamarealta.domain.events.ReservationEventListener;
import br.com.pousadamarealta.listeners.FinanceNotifier;
import br.com.pousadamarealta.listeners.GuestNotifier;
import br.com.pousadamarealta.listeners.HousekeepingNotifier;
import br.com.pousadamarealta.repository.MessageRepository;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;
import br.com.pousadamarealta.repository.postgres.Database;
import br.com.pousadamarealta.repository.postgres.PostgresMessageRepository;
import br.com.pousadamarealta.repository.postgres.PostgresReservationRepository;
import br.com.pousadamarealta.repository.postgres.PostgresRoomRepository;
import br.com.pousadamarealta.service.ReportService;
import br.com.pousadamarealta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.postgresql.ds.PGSimpleDataSource;

import java.util.List;

public final class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(dbUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPassword);

        Database database = new Database(dataSource);

        RoomRepository roomRepository = new PostgresRoomRepository(database);
        ReservationRepository reservationRepository = new PostgresReservationRepository(database);
        MessageRepository messageRepository = new PostgresMessageRepository(database);

        List<ReservationEventListener> listeners = List.of(
            new GuestNotifier(messageRepository),
            new HousekeepingNotifier(messageRepository),
            new FinanceNotifier(messageRepository)
        );

        ReservationService reservationService = new ReservationService(
            roomRepository,
            reservationRepository,
            listeners
        );

        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        ApiHandler apiHandler = new ApiHandler(
            roomRepository,
            reservationService,
            reportService,
            messageRepository
        );

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", apiHandler::getRooms);
        app.post("/reservations", apiHandler::createReservation);
        app.get("/reservations/{id}", apiHandler::getReservation);
        app.post("/reservations/{id}/confirmation", apiHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", apiHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", apiHandler::checkIn);
        app.post("/reservations/{id}/check-out", apiHandler::checkOut);
        app.get("/reservations/{id}/document", apiHandler::getDocument);
        app.get("/messages", apiHandler::getMessages);
        app.get("/reports/occupancy", apiHandler::getOccupancyReport);
        app.get("/reports/revenue", apiHandler::getRevenueReport);

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }
}
