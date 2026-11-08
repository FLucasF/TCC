package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.config.Database;
import com.pousada.report.OccupancyReport;
import com.pousada.report.RatePlansReport;
import com.pousada.report.RevenueReport;
import com.pousada.report.TourismTaxReport;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class ApiServer {
    private final int port;
    private final Database database;
    private Javalin app;

    public ApiServer(int port, Database database) {
        this.port = port;
        this.database = database;
    }

    public void start() {
        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PriceCalculator priceCalculator = new PriceCalculator();
        RoomService roomService = new RoomService(roomRepository);
        DocumentGenerator documentGenerator = new DocumentGenerator(roomRepository);
        MessageService messageService = new MessageService(messageRepository, documentGenerator);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, priceCalculator, messageService
        );

        OccupancyReport occupancyReport = new OccupancyReport(reservationRepository, roomRepository);
        RevenueReport revenueReport = new RevenueReport(reservationRepository);
        TourismTaxReport tourismTaxReport = new TourismTaxReport(reservationRepository);
        RatePlansReport ratePlansReport = new RatePlansReport(reservationRepository);

        app = Javalin.create(config -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.jsonMapper(new io.javalin.json.JavalinJackson(objectMapper, false));
        });

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, documentGenerator
        );
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(
            occupancyReport, revenueReport, tourismTaxReport, ratePlansReport
        );

        roomController.register(app);
        reservationController.register(app);
        messageController.register(app);
        reportController.register(app);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });

        app.start(port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}
