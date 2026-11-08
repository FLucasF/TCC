package com.pousadamarealta;

import com.pousadamarealta.api.ApiServer;
import com.pousadamarealta.notification.FinanceNotificationListener;
import com.pousadamarealta.notification.GuestNotificationListener;
import com.pousadamarealta.notification.HousekeepingNotificationListener;
import com.pousadamarealta.notification.ReservationEventListener;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;

import java.util.List;

public final class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new PostgresRoomRepository(database);
        ReservationRepository reservationRepository = new PostgresReservationRepository(database);
        MessageRepository messageRepository = new PostgresMessageRepository(database);

        MessageService messageService = new MessageService(messageRepository);

        List<ReservationEventListener> listeners = List.of(
            new GuestNotificationListener(messageService),
            new HousekeepingNotificationListener(messageService),
            new FinanceNotificationListener(messageService)
        );

        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            listeners
        );

        ReportService reportService = new ReportService(reservationService, roomRepository);

        ApiServer apiServer = new ApiServer(
            reservationService,
            roomRepository,
            messageService,
            reportService
        );

        apiServer.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }
}
