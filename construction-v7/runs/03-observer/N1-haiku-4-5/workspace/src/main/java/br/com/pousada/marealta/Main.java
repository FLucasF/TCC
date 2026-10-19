package br.com.pousada.marealta;

import br.com.pousada.marealta.api.*;
import br.com.pousada.marealta.service.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        int port = getPort();

        MessageRepository messageRepository = new MessageRepository();
        NotificationService notificationService = new NotificationService(messageRepository);
        ReservationService reservationService = new ReservationService(notificationService);
        ReportService reportService = new ReportService(reservationService);

        Javalin app = Javalin.create().start(port);

        ApiErrorHandler.register(app);
        RoomController.register(app, reservationService);
        ReservationController.register(app, reservationService);
        MessageController.register(app, messageRepository);
        ReportController.register(app, reportService);

        System.out.println("Servidor iniciado na porta " + port);
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
