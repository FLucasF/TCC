package com.pousadamarealta;

import com.pousadamarealta.api.ApiController;
import com.pousadamarealta.notification.FinanceNotifier;
import com.pousadamarealta.notification.GuestNotifier;
import com.pousadamarealta.notification.HousekeepingNotifier;
import com.pousadamarealta.notification.ReservationListener;
import com.pousadamarealta.pricing.PricingService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        int port = getPort();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();

        GuestNotifier guestNotifier = new GuestNotifier(messageRepository);
        HousekeepingNotifier housekeepingNotifier = new HousekeepingNotifier(messageRepository);
        FinanceNotifier financeNotifier = new FinanceNotifier(messageRepository);

        List<ReservationListener> listeners = List.of(
            guestNotifier,
            housekeepingNotifier,
            financeNotifier
        );

        ReservationService reservationService = new ReservationService(
            roomRepository,
            reservationRepository,
            pricingService,
            listeners
        );

        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        ApiController apiController = new ApiController(
            roomRepository,
            reservationRepository,
            messageRepository,
            reservationService,
            reportService,
            guestNotifier
        );

        Javalin app = apiController.createApp();
        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }
}
