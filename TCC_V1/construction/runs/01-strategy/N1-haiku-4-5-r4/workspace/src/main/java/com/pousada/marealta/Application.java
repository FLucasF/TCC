package com.pousada.marealta;

import com.pousada.marealta.handler.MessageHandler;
import com.pousada.marealta.handler.ReportHandler;
import com.pousada.marealta.handler.ReservationHandler;
import com.pousada.marealta.handler.RoomHandler;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomType;
import com.pousada.marealta.persistence.Database;
import com.pousada.marealta.persistence.MessageDao;
import com.pousada.marealta.persistence.ReservationDao;
import com.pousada.marealta.persistence.RoomDao;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.PricingService;
import com.pousada.marealta.service.ReportService;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.util.GsonConfig;
import com.pousada.marealta.util.JavalinGsonMapper;
import io.javalin.Javalin;

import java.math.BigDecimal;

public class Application {
    public static void main(String[] args) throws Exception {
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database.init();

        RoomDao roomDao = new RoomDao(Database.getDataSource());
        ReservationDao reservationDao = new ReservationDao(Database.getDataSource());
        MessageDao messageDao = new MessageDao(Database.getDataSource());

        MessageService messageService = new MessageService(messageDao, roomDao);
        PricingService pricingService = new PricingService();
        ReservationService reservationService = new ReservationService(reservationDao, roomDao, pricingService, messageService);
        ReportService reportService = new ReportService(reservationDao, roomDao);

        RoomHandler roomHandler = new RoomHandler(roomDao);
        ReservationHandler reservationHandler = new ReservationHandler(reservationService, messageService);
        MessageHandler messageHandler = new MessageHandler(messageDao);
        ReportHandler reportHandler = new ReportHandler(reportService);

        initializeRooms(roomDao);

        Javalin app = Javalin.create(config ->
            config.jsonMapper(new JavalinGsonMapper())
        ).start(port);

        app.get("/rooms", roomHandler::listRooms);

        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkIn);
        app.post("/reservations/{id}/check-out", ctx -> {
            ctx.attribute("roomDao", roomDao);
            reservationHandler.checkOut(ctx);
        });
        app.get("/reservations/{id}/document", ctx -> {
            ctx.attribute("roomDao", roomDao);
            reservationHandler.getReceipt(ctx);
        });

        app.get("/messages", messageHandler::listMessages);

        app.get("/reports/occupancy", reportHandler::occupancyReport);
        app.get("/reports/revenue", reportHandler::revenueReport);

        System.out.println("Server started on port " + port);
    }

    private static void initializeRooms(RoomDao roomDao) throws Exception {
        Room[] rooms = {
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"))
        };

        for (Room room : rooms) {
            if (roomDao.findByNumber(room.getNumber()) == null) {
                roomDao.save(room);
            }
        }
    }
}
