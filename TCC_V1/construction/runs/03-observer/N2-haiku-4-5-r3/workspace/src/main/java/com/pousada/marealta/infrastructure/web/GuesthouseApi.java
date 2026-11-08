package com.pousada.marealta.infrastructure.web;

import com.pousada.marealta.application.reservation.ReservationService;
import com.pousada.marealta.domain.notification.NotificationSubscriber;
import com.pousada.marealta.domain.repository.NotificationRepository;
import com.pousada.marealta.domain.repository.ReservationRepository;
import com.pousada.marealta.domain.repository.RoomRepository;
import com.pousada.marealta.infrastructure.notification.FinanceNotifier;
import com.pousada.marealta.infrastructure.notification.GovernanceNotifier;
import com.pousada.marealta.infrastructure.notification.GuestNotifier;
import com.pousada.marealta.infrastructure.web.handler.MessagesHandler;
import com.pousada.marealta.infrastructure.web.handler.ReportsHandler;
import com.pousada.marealta.infrastructure.web.handler.ReservationsHandler;
import com.pousada.marealta.infrastructure.web.handler.RoomsHandler;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class GuesthouseApi {
    private static final Logger logger = LoggerFactory.getLogger(GuesthouseApi.class);

    private final Javalin app;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final ReservationService reservationService;

    public GuesthouseApi(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            NotificationRepository notificationRepository
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;

        List<NotificationSubscriber> subscribers = Arrays.asList(
                new GuestNotifier(),
                new GovernanceNotifier(),
                new FinanceNotifier()
        );

        this.reservationService = new ReservationService(
                reservationRepository,
                roomRepository,
                notificationRepository,
                subscribers
        );

        this.app = createApp();
    }

    private Javalin createApp() {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        });

        app.get("/health", ctx -> ctx.result("OK"));

        // Rooms endpoints
        RoomsHandler roomsHandler = new RoomsHandler(roomRepository);
        app.get("/rooms", roomsHandler::listRooms);

        // Reservations endpoints
        ReservationsHandler reservationsHandler = new ReservationsHandler(reservationService);
        app.post("/reservations", reservationsHandler::createReservation);
        app.get("/reservations/{id}", reservationsHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationsHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationsHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationsHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationsHandler::checkOut);
        app.get("/reservations/{id}/document", reservationsHandler::getDocument);

        // Messages endpoint
        MessagesHandler messagesHandler = new MessagesHandler(notificationRepository);
        app.get("/messages", messagesHandler::listMessages);

        // Reports endpoints
        ReportsHandler reportsHandler = new ReportsHandler(reservationRepository, roomRepository);
        app.get("/reports/occupancy", reportsHandler::occupancyReport);
        app.get("/reports/revenue", reportsHandler::revenueReport);

        return app;
    }

    public void start(int port) {
        logger.info("Starting Guesthouse API on port {}", port);
        app.start(port);
    }

    public void stop() {
        app.stop();
    }

    public Javalin getApp() {
        return app;
    }
}
