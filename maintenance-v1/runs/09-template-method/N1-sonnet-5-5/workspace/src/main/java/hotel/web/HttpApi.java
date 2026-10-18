package hotel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hotel.messages.SentMessages;
import hotel.reports.Report;
import hotel.reservations.ErrorCode;
import hotel.reservations.Guest;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.ReservationService;
import hotel.rooms.Rooms;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.util.List;

/** The HTTP API the inn's website and front desk call. */
public final class HttpApi {

    private final Javalin server;
    private final Rooms rooms;
    private final ReservationService reservations;
    private final SentMessages sentMessages;
    private final List<Report> reports;

    public HttpApi(
            Rooms rooms,
            ReservationService reservations,
            SentMessages sentMessages,
            List<Report> reports) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.sentMessages = sentMessages;
        this.reports = reports;

        ObjectMapper jsonMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        this.server = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(jsonMapper, false));
        });
        registerRoutes();
        registerErrorHandling();
    }

    public Javalin server() {
        return server;
    }

    private void registerRoutes() {
        server.get("/rooms", ctx -> ctx.json(rooms.all()));

        server.post("/reservations", ctx -> {
            NewReservation request = readNewReservation(ctx);
            ctx.status(201).json(new ReservationResponse(reservations.create(request)));
        });
        server.get("/reservations/{id}", ctx ->
                ctx.json(new ReservationResponse(reservations.find(reservationId(ctx)))));
        server.post("/reservations/{id}/confirmation", ctx ->
                ctx.json(new ReservationResponse(reservations.confirm(reservationId(ctx)))));
        server.post("/reservations/{id}/cancellation", ctx ->
                ctx.json(new ReservationResponse(reservations.cancel(reservationId(ctx)))));
        server.post("/reservations/{id}/check-in", ctx ->
                ctx.json(new ReservationResponse(reservations.checkIn(reservationId(ctx)))));
        server.post("/reservations/{id}/check-out", ctx ->
                ctx.json(reservations.checkOut(reservationId(ctx))));
        server.get("/reservations/{id}/document", ctx ->
                ctx.json(reservations.receiptCopy(reservationId(ctx))));

        server.get("/messages", ctx -> ctx.json(sentMessages.all()));

        for (Report report : reports) {
            server.get("/reports/" + report.name(), ctx -> {
                String period = requiredQueryParam(ctx, report.periodParameter());
                ctx.contentType("text/csv; charset=utf-8").result(report.generateFor(period));
            });
        }
    }

    private void registerErrorHandling() {
        server.exception(HotelException.class, (exception, ctx) -> {
            ctx.status(httpStatus(exception.errorCode()));
            ctx.json(new ErrorResponse(exception.errorCode().name(), exception.getMessage()));
        });
    }

    private static int httpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_REQUEST -> 400;
            case ROOM_NOT_FOUND, RESERVATION_NOT_FOUND -> 404;
            case ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED -> 409;
            case CAPACITY_EXCEEDED -> 422;
        };
    }

    private static NewReservation readNewReservation(Context ctx) {
        CreateReservationRequest body;
        try {
            body = ctx.bodyAsClass(CreateReservationRequest.class);
        } catch (Exception unreadableBody) { // Jackson's own exceptions are checked, but reach here unwrapped
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the body is not a valid reservation request");
        }
        if (body == null
                || body.roomNumber() == null
                || isBlank(body.guestName())
                || isBlank(body.guestEmail())
                || isBlank(body.guestCpf())
                || body.checkIn() == null
                || body.checkOut() == null
                || body.guests() == null
                || body.ratePlan() == null) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "every field of the reservation is required");
        }
        Guest guest = new Guest(body.guestName(), body.guestEmail(), body.guestCpf());
        return new NewReservation(
                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan());
    }

    private static long reservationId(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException notANumber) {
            throw new HotelException(ErrorCode.RESERVATION_NOT_FOUND, "reservation " + id + " does not exist");
        }
    }

    private static String requiredQueryParam(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the query parameter '" + name + "' is required");
        }
        return value;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
