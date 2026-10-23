package br.com.marealto;

import br.com.marealto.handler.ApiHandler;
import br.com.marealto.repository.Database;
import br.com.marealto.repository.MessageRepository;
import br.com.marealto.repository.ReservationRepository;
import br.com.marealto.repository.RoomRepository;
import br.com.marealto.service.DocumentService;
import br.com.marealto.service.PricingService;
import br.com.marealto.service.ReportService;
import br.com.marealto.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null) {
            dbUrl = "jdbc:postgresql://localhost:5432/pousada";
        }
        if (dbUser == null) {
            dbUser = "postgres";
        }
        if (dbPassword == null) {
            dbPassword = "postgres";
        }

        int port = 7070;
        if (portStr != null) {
            port = Integer.parseInt(portStr);
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository();
        roomRepository.initializeRooms();

        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService(pricingService);
        ReservationService reservationService = new ReservationService(
                roomRepository,
                reservationRepository,
                messageRepository,
                pricingService,
                documentService
        );
        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        ApiHandler apiHandler = new ApiHandler(
                roomRepository,
                messageRepository,
                reservationService,
                documentService,
                reportService
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.shutdown();
        }));

        System.out.println("Pousada Maré Alta sistema iniciado na porta " + port);
    }
}
