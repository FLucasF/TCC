package com.pousadamareaalta;

import com.pousadamareaalta.api.ApiHandlers;
import com.pousadamareaalta.service.*;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        int port = getPort();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);
        ApiHandlers handlers = new ApiHandlers(
            reservationService,
            roomRepository,
            reservationRepository,
            messageService,
            reportService
        );

        Javalin app = Javalin.create()
            .start(port);

        app.get("/rooms", handlers::getRooms);
        app.post("/reservations", handlers::createReservation);
        app.get("/reservations/{id}", handlers::getReservation);
        app.post("/reservations/{id}/confirmation", handlers::confirmReservation);
        app.post("/reservations/{id}/cancellation", handlers::cancelReservation);
        app.post("/reservations/{id}/check-in", handlers::checkin);
        app.post("/reservations/{id}/check-out", handlers::checkout);
        app.get("/reservations/{id}/document", handlers::getDocument);
        app.get("/messages", handlers::getMessages);
        app.get("/reports/occupancy", handlers::getOccupancyReport);
        app.get("/reports/revenue", handlers::getRevenueReport);

        System.out.println("Servidor rodando na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }
}
