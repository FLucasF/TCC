package br.com.maralta;

import br.com.maralta.controller.*;
import br.com.maralta.database.Database;
import br.com.maralta.repository.MessageRepository;
import br.com.maralta.repository.ReservationRepository;
import br.com.maralta.repository.RoomRepository;
import br.com.maralta.service.MessageService;
import br.com.maralta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.sql.Connection;

public class Application {

    public static void main(String[] args) {
        int port = getPort();

        Connection connection = Database.getConnection();

        RoomRepository roomRepository = new RoomRepository(connection);
        ReservationRepository reservationRepository = new ReservationRepository(connection);
        MessageRepository messageRepository = new MessageRepository(connection);

        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService, roomRepository);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reservationService, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        ExceptionHandler.register(app);

        app.get("/rooms", roomController::listRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::listMessages);

        app.get("/reports/occupancy", reportController::occupancyReport);
        app.get("/reports/revenue", reportController::revenueReport);

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.close();
        }));
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
            }
        }
        return 7070;
    }
}
