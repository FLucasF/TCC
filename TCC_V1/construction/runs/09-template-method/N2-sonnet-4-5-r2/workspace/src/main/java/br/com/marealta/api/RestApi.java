package br.com.marealta.api;

import br.com.marealta.domain.*;
import br.com.marealta.report.*;
import br.com.marealta.repository.*;
import br.com.marealta.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class RestApi {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final ApiMapper apiMapper;

    public RestApi(RoomRepository roomRepository,
                   ReservationRepository reservationRepository,
                   MessageRepository messageRepository,
                   ReservationService reservationService,
                   MessageService messageService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.apiMapper = new ApiMapper(objectMapper);
    }

    public Javalin createApp() {
        Javalin app = Javalin.create();

        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
        app.get("/reports/tourism-tax", this::getTourismTaxReport);
        app.get("/reports/rate-plans", this::getRatePlansReport);

        app.exception(ApiMapper.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation status"));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        });

        return app;
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ArrayNode array = objectMapper.createArrayNode();
        for (Room room : rooms) {
            array.add(apiMapper.mapRoom(room));
        }
        ctx.json(array);
    }

    private void createReservation(Context ctx) {
        ApiMapper.CreateReservationRequest request =
                apiMapper.parseCreateReservationRequest(ctx.body());

        Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                request.ratePlan()
        );

        ctx.status(201).json(apiMapper.mapReservation(reservation));
    }

    private void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(apiMapper.mapReservation(reservation));
    }

    private void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(apiMapper.mapReservation(reservation));
    }

    private void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(apiMapper.mapReservation(reservation));
    }

    private void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(apiMapper.mapReservation(reservation));
    }

    private void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(apiMapper.mapDocument(document));
    }

    private void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(apiMapper.mapDocument(document));
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        ArrayNode array = objectMapper.createArrayNode();
        for (Message message : messages) {
            array.add(apiMapper.mapMessage(message));
        }
        ctx.json(array);
    }

    private void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new ApiMapper.InvalidRequestException("Missing date parameter");
        }
        LocalDate date = apiMapper.parseDate(dateParam);
        CsvReport report = new OccupancyReport(date, reservationRepository, roomRepository);
        ctx.contentType("text/csv; charset=utf-8").result(report.generate());
    }

    private void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new ApiMapper.InvalidRequestException("Missing month parameter");
        }
        YearMonth month = apiMapper.parseMonth(monthParam);
        CsvReport report = new RevenueReport(month, reservationRepository);
        ctx.contentType("text/csv; charset=utf-8").result(report.generate());
    }

    private void getTourismTaxReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new ApiMapper.InvalidRequestException("Missing month parameter");
        }
        YearMonth month = apiMapper.parseMonth(monthParam);
        CsvReport report = new TourismTaxReport(month, reservationRepository);
        ctx.contentType("text/csv; charset=utf-8").result(report.generate());
    }

    private void getRatePlansReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new ApiMapper.InvalidRequestException("Missing month parameter");
        }
        YearMonth month = apiMapper.parseMonth(monthParam);
        CsvReport report = new RatePlanReport(month, reservationRepository);
        ctx.contentType("text/csv; charset=utf-8").result(report.generate());
    }
}
