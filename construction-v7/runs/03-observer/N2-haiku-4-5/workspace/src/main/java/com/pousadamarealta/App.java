package com.pousadamarealta;

import com.pousadamarealta.api.ReservationApi;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.ReportService;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        int port = getPort();

        ReservationRepository reservationRepo = new ReservationRepository();
        RoomRepository roomRepo = new RoomRepository();
        MessageRepository messageRepo = new MessageRepository();
        PricingService pricingService = new PricingService();

        ReservationService reservationService = new ReservationService(
            reservationRepo,
            roomRepo,
            messageRepo,
            pricingService
        );

        ReportService reportService = new ReportService(reservationRepo, roomRepo);

        Javalin app = Javalin.create().start(port);

        ReservationApi api = new ReservationApi(
            app,
            reservationService,
            reportService,
            roomRepo,
            messageRepo
        );
        api.setup();

        System.out.println("Servidor rodando na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }
}
