package br.com.pousadamareal;

import br.com.pousadamareal.api.handlers.MessageHandler;
import br.com.pousadamareal.api.handlers.ReportHandler;
import br.com.pousadamareal.api.handlers.ReservationHandler;
import br.com.pousadamareal.api.handlers.RoomHandler;
import br.com.pousadamareal.db.DatabaseConnection;
import br.com.pousadamareal.db.SchemaInitializer;
import br.com.pousadamareal.repository.MessageRepository;
import br.com.pousadamareal.repository.ReservationRepository;
import br.com.pousadamareal.repository.RoomRepository;
import br.com.pousadamareal.service.DocumentService;
import br.com.pousadamareal.service.PricingService;
import br.com.pousadamareal.service.ReportService;
import br.com.pousadamareal.service.ReservationService;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String port = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new Exception("Missing database environment variables");
        }

        int portNumber = port != null ? Integer.parseInt(port) : 7070;

        DatabaseConnection.initialize(dbUrl, dbUser, dbPassword);
        SchemaInitializer.initialize();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        DocumentService documentService = new DocumentService(roomRepository);
        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                roomRepository,
                messageRepository,
                documentService,
                pricingService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomHandler roomHandler = new RoomHandler(roomRepository);
        ReservationHandler reservationHandler = new ReservationHandler(reservationService);
        MessageHandler messageHandler = new MessageHandler(messageRepository);
        ReportHandler reportHandler = new ReportHandler(reportService);

        Javalin app = Javalin.create();

        app.get("/rooms", roomHandler::getAllRooms);

        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOut);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);

        app.get("/messages", messageHandler::getAllMessages);

        app.get("/reports/occupancy", reportHandler::getOccupancyReport);
        app.get("/reports/revenue", reportHandler::getRevenueReport);

        app.start(portNumber);
    }
}
