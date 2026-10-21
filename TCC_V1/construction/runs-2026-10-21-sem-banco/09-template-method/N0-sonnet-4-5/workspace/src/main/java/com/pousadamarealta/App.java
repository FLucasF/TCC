package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.ApiHandler;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        int port = getPort();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();

        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, messageRepository, pricingService);

        ReportService reportService = new ReportService(
                reservationRepository, roomRepository, pricingService);

        ApiHandler handler = new ApiHandler(
                roomRepository, reservationService, reservationRepository, messageRepository, reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.get("/rooms", handler::getRooms);
        app.post("/reservations", handler::createReservation);
        app.get("/reservations/{id}", handler::getReservation);
        app.post("/reservations/{id}/confirmation", handler::confirmReservation);
        app.post("/reservations/{id}/cancellation", handler::cancelReservation);
        app.post("/reservations/{id}/check-in", handler::checkIn);
        app.post("/reservations/{id}/check-out", handler::checkOut);
        app.get("/reservations/{id}/document", handler::getDocument);
        app.get("/messages", handler::getMessages);
        app.get("/reports/occupancy", handler::getOccupancyReport);
        app.get("/reports/revenue", handler::getRevenueReport);
        app.get("/reports/tourism-tax", handler::getTourismTaxReport);
        app.get("/reports/rate-plans", handler::getRatePlansReport);

        app.exception(Exception.class, (e, ctx) -> handler.handleException(ctx, e));

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida na variável PORT: " + portEnv);
            }
        }
        return 7070;
    }
}
