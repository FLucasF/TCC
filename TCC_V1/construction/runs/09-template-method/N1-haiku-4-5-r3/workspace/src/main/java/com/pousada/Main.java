package com.pousada;

import com.pousada.api.MessageController;
import com.pousada.api.ReportController;
import com.pousada.api.ReservationController;
import com.pousada.api.RoomController;
import com.pousada.config.DatabaseConfig;
import com.pousada.exception.ApiException;
import com.pousada.repository.RoomRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        int port = getPort();

        DatabaseConfig.initialize();
        RoomRepository.initializeRooms();

        Javalin app = Javalin.create()
            .exception(ApiException.class, (e, ctx) -> handleApiException(e, ctx))
            .exception(Exception.class, (e, ctx) -> handleGenericException(e, ctx));

        app.get("/rooms", RoomController::getRooms);

        app.post("/reservations", ReservationController::createReservation);
        app.get("/reservations/{id}", ReservationController::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationController::confirmReservation);
        app.post("/reservations/{id}/check-in", ReservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", ReservationController::checkOutReservation);
        app.post("/reservations/{id}/cancellation", ReservationController::cancelReservation);
        app.get("/reservations/{id}/document", ReservationController::getDocument);

        app.get("/messages", MessageController::getMessages);

        app.get("/reports/occupancy", ReportController::getOccupancyReport);
        app.get("/reports/revenue", ReportController::getRevenueReport);
        app.get("/reports/tourism-tax", ReportController::getTourismTaxReport);
        app.get("/reports/rate-plans", ReportController::getRatePlansReport);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            DatabaseConfig.close();
        }));

        System.out.println("Server started on port " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }

    private static void handleApiException(ApiException e, Context ctx) {
        ctx.status(e.getStatusCode());
        ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    private static void handleGenericException(Exception e, Context ctx) {
        System.err.println("Unhandled exception: " + e.getMessage());
        e.printStackTrace();
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static class ErrorResponse {
        public String error;
        public String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
