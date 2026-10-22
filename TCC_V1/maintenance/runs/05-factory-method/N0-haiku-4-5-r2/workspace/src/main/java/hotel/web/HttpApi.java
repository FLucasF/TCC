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
            hotel.reservations.Reservation created = reservations.create(request);
            ctx.status(201).json(buildReservationResponse(created, reservations));
        });
        server.get("/reservations/{id}", ctx -> {
            hotel.reservations.Reservation reservation = reservations.find(reservationId(ctx));
            ctx.json(buildReservationResponse(reservation, reservations));
        });
        server.post("/reservations/{id}/confirmation", ctx -> {
            hotel.reservations.Reservation confirmed = reservations.confirm(reservationId(ctx));
            ctx.json(buildReservationResponse(confirmed, reservations));
        });
        server.post("/reservations/{id}/cancellation", ctx -> {
            hotel.reservations.Reservation cancelled = reservations.cancel(reservationId(ctx));
            ctx.json(buildReservationResponse(cancelled, reservations));
        });
        server.post("/reservations/{id}/check-in", ctx -> {
            hotel.reservations.Reservation checkedIn = reservations.checkIn(reservationId(ctx));
            ctx.json(buildReservationResponse(checkedIn, reservations));
        });
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

        boolean hasCpf = !isBlank(body.guestCpf());
        boolean hasCompany = !isBlank(body.companyCnpj()) || !isBlank(body.companyName());
        boolean hasPassport = !isBlank(body.guestPassport()) || !isBlank(body.guestCountry());

        int identificationCount = (hasCpf ? 1 : 0) + (hasCompany ? 1 : 0) + (hasPassport ? 1 : 0);

        if (identificationCount != 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "must provide exactly one of: guestCpf, (companyCnpj and companyName), or (guestPassport and guestCountry)");
        }

        Guest guest;
        if (hasCpf) {
            guest = Guest.withCpf(body.guestName(), body.guestEmail(), body.guestCpf());
        } else if (hasCompany) {
            if (isBlank(body.companyCnpj()) || isBlank(body.companyName())) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "companyCnpj and companyName must both be provided");
            }
            guest = Guest.withCompany(body.guestName(), body.guestEmail(), body.companyCnpj(), body.companyName());
        } else {
            if (isBlank(body.guestPassport()) || isBlank(body.guestCountry())) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "guestPassport and guestCountry must both be provided");
            }
            guest = Guest.withPassport(body.guestName(), body.guestEmail(), body.guestPassport(), body.guestCountry());
        }

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

    private static ReservationResponse buildReservationResponse(
            hotel.reservations.Reservation reservation, ReservationService reservationService) {
        String documentText = reservationService.getDocumentText(reservation);
        return new ReservationResponse(reservation, documentText);
    }
}
