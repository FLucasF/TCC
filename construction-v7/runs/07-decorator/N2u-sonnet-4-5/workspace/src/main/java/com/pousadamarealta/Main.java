package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.domain.ReportService;
import com.pousadamarealta.domain.ReservationService;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.web.HotelController;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        // Verificar modo de teste
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        // Determinar porta
        int port = 7070;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable: " + portEnv);
            }
        }

        // Configurar Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Inicializar repositórios e serviços
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageService messageService = new MessageService(testMode);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        HotelController controller = new HotelController(
                roomRepository, reservationRepository, reservationService, messageService, reportService);

        // Criar aplicação Javalin
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(port);

        // Registrar rotas
        app.get("/rooms", controller::getRooms);
        app.post("/reservations", controller::createReservation);
        app.get("/reservations/{id}", controller::getReservation);
        app.post("/reservations/{id}/confirmation", controller::confirmReservation);
        app.post("/reservations/{id}/cancellation", controller::cancelReservation);
        app.post("/reservations/{id}/check-in", controller::checkIn);
        app.post("/reservations/{id}/check-out", controller::checkOut);
        app.get("/reservations/{id}/document", controller::getDocument);
        app.get("/messages", controller::getMessages);
        app.get("/reports/occupancy", controller::getOccupancyReport);
        app.get("/reports/revenue", controller::getRevenueReport);

        System.out.println("Hotel Reservation System started on port " + port);
        System.out.println("Test mode: " + testMode);
    }
}
