package hotel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hotel.messages.SentMessages;
import hotel.reports.OccupancyReport;
import hotel.reports.RevenueReport;
import hotel.reservations.ErrorCode;
import hotel.reservations.Guest;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.ReservationService;
import hotel.rooms.Rooms;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** The HTTP API the inn's website and front desk call. */
public final class HttpApi {

    private final Javalin server;
    private final Rooms rooms;
    private final ReservationService reservations;
    private final SentMessages sentMessages;
    private final OccupancyReport occupancyReport;
    private final RevenueReport revenueReport;

    public HttpApi(
            Rooms rooms,
            ReservationService reservations,
            SentMessages sentMessages,
            OccupancyReport occupancyReport,
            RevenueReport revenueReport) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.sentMessages = sentMessages;
        this.occupancyReport = occupancyReport;
        this.revenueReport = revenueReport;

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

        server.get("/reports/occupancy", ctx -> {
            LocalDate night = readDate(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(occupancyReport.generate(night));
        });
        server.get("/reports/revenue", ctx -> {
            YearMonth month = readMonth(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(revenueReport.generate(month));
        });
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
                || body.checkIn() == null
                || body.checkOut() == null
                || body.guests() == null
                || body.ratePlan() == null) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "every field of the reservation is required");
        }
        Guest guest = readGuest(body);
        return new NewReservation(
                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan());
    }

    /** A CPF, or a CNPJ with the company name, or a passport with the country: one of them, and only one. */
    private static Guest readGuest(CreateReservationRequest body) {
        boolean cpf = !isBlank(body.guestCpf());
        boolean company = !isBlank(body.companyCnpj()) || !isBlank(body.companyName());
        boolean passport = !isBlank(body.guestPassport()) || !isBlank(body.guestCountry());
        boolean oneKind = (cpf ? 1 : 0) + (company ? 1 : 0) + (passport ? 1 : 0) == 1;
        boolean complete = cpf
                || (company && !isBlank(body.companyCnpj()) && !isBlank(body.companyName()))
                || (passport && !isBlank(body.guestPassport()) && !isBlank(body.guestCountry()));
        if (!oneKind || !complete) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "identify the guest with exactly one of: guestCpf; companyCnpj with companyName; "
                            + "guestPassport with guestCountry");
        }
        if (cpf) {
            return new Guest(body.guestName(), body.guestEmail(), body.guestCpf());
        }
        if (company) {
            return Guest.ofCompany(body.guestName(), body.guestEmail(), body.companyCnpj(), body.companyName());
        }
        return Guest.ofForeigner(body.guestName(), body.guestEmail(), body.guestPassport(), body.guestCountry());
    }

    private static long reservationId(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException notANumber) {
            throw new HotelException(ErrorCode.RESERVATION_NOT_FOUND, "reservation " + id + " does not exist");
        }
    }

    private static LocalDate readDate(Context ctx) {
        String date = requiredQueryParam(ctx, "date");
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException badDate) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "'" + date + "' is not a date (yyyy-MM-dd)");
        }
    }

    private static YearMonth readMonth(Context ctx) {
        String month = requiredQueryParam(ctx, "month");
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException badMonth) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "'" + month + "' is not a month (yyyy-MM)");
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
