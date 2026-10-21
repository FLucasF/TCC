package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.api.ReportsApiHandler;
import com.pousada.api.ReservationsApiHandler;
import com.pousada.api.RoomsApiHandler;
import com.pousada.api.MessagesApiHandler;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.PricingService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        RoomRepository roomRepo = new RoomRepository();
        ReservationRepository reservationRepo = new ReservationRepository();
        MessageRepository messageRepo = new MessageRepository();
        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(
            reservationRepo, roomRepo, messageRepo, pricingService
        );
        ReportService reportService = new ReportService(reservationRepo, roomRepo);

        Javalin app = Javalin.create();

        RoomsApiHandler.register(app, roomRepo);
        ReservationsApiHandler.register(app, reservationService, reservationRepo);
        MessagesApiHandler.register(app, messageRepo);
        ReportsApiHandler.register(app, reportService);

        String port = System.getenv("PORT");
        int portNumber = port != null ? Integer.parseInt(port) : 7070;

        app.start(portNumber);
        System.out.println("Pousada Maré Alta - Sistema de Reservas iniciado na porta " + portNumber);
    }
}
