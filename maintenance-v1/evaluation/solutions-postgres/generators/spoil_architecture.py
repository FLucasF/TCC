"""Calibration examples for the architecture column of the quality reviewer (TCC_V1/evaluation/quality): two
reference solutions with their architecture spoiled on purpose. The behavior stays the same.

- architecture-mixed (expected `com problemas`): 01-pattern with the check-in done inside the HTTP route, with its
  own SQL and its own copy of the rule (only a confirmed reservation checks in); the service's checkIn is left
  unused. One localized mix of layers.
- architecture-routes (expected `ruim`): 09-pattern without the reservation service: every route of the
  reservations does the rule and the SQL itself (create, confirm, cancel, check-in, check-out, the copy of the
  receipt), the refund rule included. Only the reads go through the store. The service's tests go with it.
"""

from kit import HOTEL, HTTP_API, SERVICE, SERVICE_TEST, Solution

mixed = Solution("architecture-mixed")
mixed.apply("01-pattern")
mixed.replace(HTTP_API, "import io.javalin.json.JavalinJackson;\n",
              "import io.javalin.json.JavalinJackson;\nimport java.sql.Connection;\nimport java.sql.PreparedStatement;\n")
mixed.replace(HTTP_API, "import java.time.format.DateTimeParseException;\n",
              "import java.time.format.DateTimeParseException;\nimport javax.sql.DataSource;\n")
mixed.replace(HTTP_API, """    private final Javalin server;
""", """    private final Javalin server;
    private final DataSource dataSource;
""")
mixed.replace(HTTP_API, """    public HttpApi(
            Rooms rooms,""", """    public HttpApi(
            DataSource dataSource,
            Rooms rooms,""")
mixed.replace(HTTP_API, """        this.rooms = rooms;
""", """        this.dataSource = dataSource;
        this.rooms = rooms;
""")
mixed.replace(HTTP_API, """        server.post("/reservations/{id}/check-in", ctx ->
                ctx.json(new ReservationResponse(reservations.checkIn(reservationId(ctx)))));""",
              """        server.post("/reservations/{id}/check-in", ctx -> {
            long id = reservationId(ctx);
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement update = connection.prepareStatement(
                            "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ? AND status = 'CONFIRMED'")) {
                update.setLong(1, id);
                if (update.executeUpdate() == 0) {
                    throw new HotelException(ErrorCode.OPERATION_NOT_ALLOWED, "cannot check in reservation " + id
                            + " while it is " + reservations.find(id).status());
                }
            }
            ctx.json(new ReservationResponse(reservations.find(id)));
        });""")
mixed.replace(HOTEL, """        HttpApi api = new HttpApi(
                rooms, reservations,""", """        HttpApi api = new HttpApi(
                dataSource, rooms, reservations,""")
mixed.build_and_save()

ROUTES_API = '''package hotel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reports.OccupancyReport;
import hotel.reports.RatePlanReport;
import hotel.reports.RevenueReport;
import hotel.reports.TourismTaxReport;
import hotel.reservations.ErrorCode;
import hotel.reservations.Guest;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import javax.sql.DataSource;

/** The HTTP API the inn's website and front desk call. */
public final class HttpApi {

    private static final String HOTEL_NAME = "Pousada Maré Alta";

    private final Javalin server;
    private final DataSource dataSource;
    private final Rooms rooms;
    private final ReservationStore store;
    private final EmailSender emailSender;
    private final SentMessages sentMessages;
    private final OccupancyReport occupancyReport;
    private final RevenueReport revenueReport;
    private final TourismTaxReport tourismTaxReport;
    private final RatePlanReport ratePlanReport;

    public HttpApi(
            DataSource dataSource,
            Rooms rooms,
            ReservationStore store,
            EmailSender emailSender,
            SentMessages sentMessages,
            OccupancyReport occupancyReport,
            RevenueReport revenueReport,
            TourismTaxReport tourismTaxReport,
            RatePlanReport ratePlanReport) {
        this.dataSource = dataSource;
        this.rooms = rooms;
        this.store = store;
        this.emailSender = emailSender;
        this.sentMessages = sentMessages;
        this.occupancyReport = occupancyReport;
        this.revenueReport = revenueReport;
        this.tourismTaxReport = tourismTaxReport;
        this.ratePlanReport = ratePlanReport;

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
            if (!request.checkOut().isAfter(request.checkIn())) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "check-out must be after check-in");
            }
            if (request.guests() < 1) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "a reservation needs at least one guest");
            }
            Room room = rooms.find(request.roomNumber()).orElseThrow(() -> new HotelException(
                    ErrorCode.ROOM_NOT_FOUND, "room " + request.roomNumber() + " does not exist"));
            if (request.guests() > room.capacity()) {
                throw new HotelException(ErrorCode.CAPACITY_EXCEEDED,
                        "room " + room.number() + " takes at most " + room.capacity() + " guests");
            }
            long id;
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement taken = connection.prepareStatement("SELECT 1 FROM reservations WHERE "
                        + "room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND ? < check_out")) {
                    taken.setInt(1, room.number());
                    taken.setDate(2, Date.valueOf(request.checkOut()));
                    taken.setDate(3, Date.valueOf(request.checkIn()));
                    try (ResultSet rows = taken.executeQuery()) {
                        if (rows.next()) {
                            throw new HotelException(ErrorCode.ROOM_UNAVAILABLE,
                                    "room " + room.number() + " is taken on some of those nights");
                        }
                    }
                }
                Price price = new PriceCalculator().calculate(
                        room, request.checkIn(), request.checkOut(), request.ratePlan());
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO reservations (room_number, "
                        + "guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, "
                        + "subtotal, discount, total, status, amount_paid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0.00) RETURNING id")) {
                    insert.setInt(1, room.number());
                    insert.setString(2, request.guest().name());
                    insert.setString(3, request.guest().email());
                    insert.setString(4, request.guest().cpf());
                    insert.setDate(5, Date.valueOf(request.checkIn()));
                    insert.setDate(6, Date.valueOf(request.checkOut()));
                    insert.setInt(7, request.guests());
                    insert.setString(8, request.ratePlan().name());
                    insert.setInt(9, price.nights());
                    insert.setBigDecimal(10, price.subtotal());
                    insert.setBigDecimal(11, price.discount());
                    insert.setBigDecimal(12, price.total());
                    try (ResultSet generated = insert.executeQuery()) {
                        generated.next();
                        id = generated.getLong("id");
                    }
                } catch (SQLException failure) {
                    if ("23P01".equals(failure.getSQLState())) {
                        throw new HotelException(ErrorCode.ROOM_UNAVAILABLE,
                                "room " + room.number() + " is taken on some of those nights");
                    }
                    throw failure;
                }
            }
            ctx.status(201).json(new ReservationResponse(find(id)));
        });
        server.get("/reservations/{id}", ctx -> ctx.json(new ReservationResponse(find(reservationId(ctx)))));
        server.post("/reservations/{id}/confirmation", ctx -> {
            Reservation reservation = find(reservationId(ctx));
            if (reservation.status() != ReservationStatus.PENDING) {
                throw notAllowed(reservation, "confirm");
            }
            update(reservation.id(), "UPDATE reservations SET status = 'CONFIRMED', amount_paid = total WHERE id = ?");
            Reservation confirmed = find(reservation.id());
            ReservationReceipt receipt = new ReservationReceipt(confirmed, roomOf(confirmed));
            emailSender.send(confirmed.guest().email(), "Reserva confirmada - " + HOTEL_NAME,
                    "Olá, " + confirmed.guest().name() + "!\\n\\n"
                            + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\\n\\n"
                            + receipt.text() + "\\n"
                            + "A entrada é a partir das 14h, e a saída, até as 12h.\\n"
                            + "Até breve!\\n");
            ctx.json(new ReservationResponse(confirmed));
        });
        server.post("/reservations/{id}/cancellation", ctx -> {
            Reservation reservation = find(reservationId(ctx));
            if (reservation.status() != ReservationStatus.PENDING
                    && reservation.status() != ReservationStatus.CONFIRMED) {
                throw notAllowed(reservation, "cancel");
            }
            BigDecimal refund = reservation.ratePlan() == RatePlan.STANDARD
                    ? reservation.amountPaid() : new BigDecimal("0.00");
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement cancel = connection.prepareStatement(
                            "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?")) {
                cancel.setBigDecimal(1, refund);
                cancel.setLong(2, reservation.id());
                cancel.executeUpdate();
            }
            ctx.json(new ReservationResponse(find(reservation.id())));
        });
        server.post("/reservations/{id}/check-in", ctx -> {
            Reservation reservation = find(reservationId(ctx));
            if (reservation.status() != ReservationStatus.CONFIRMED) {
                throw notAllowed(reservation, "check in");
            }
            update(reservation.id(), "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?");
            ctx.json(new ReservationResponse(find(reservation.id())));
        });
        server.post("/reservations/{id}/check-out", ctx -> {
            Reservation reservation = find(reservationId(ctx));
            if (reservation.status() != ReservationStatus.CHECKED_IN) {
                throw notAllowed(reservation, "check out");
            }
            update(reservation.id(), "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?");
            Reservation checkedOut = find(reservation.id());
            ctx.json(new ReservationReceipt(checkedOut, roomOf(checkedOut)));
        });
        server.get("/reservations/{id}/document", ctx -> {
            Reservation reservation = find(reservationId(ctx));
            if (!reservation.isPaid()) {
                throw notAllowed(reservation, "issue a receipt for");
            }
            ctx.json(new ReservationReceipt(reservation, roomOf(reservation)));
        });

        server.get("/messages", ctx -> ctx.json(sentMessages.all()));

        server.get("/reports/occupancy", ctx -> {
            LocalDate night = readDate(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(occupancyReport.generate(night));
        });
        server.get("/reports/revenue", ctx -> {
            YearMonth month = readMonth(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(revenueReport.generate(month));
        });
        server.get("/reports/tourism-tax", ctx -> {
            YearMonth month = readMonth(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(tourismTaxReport.generate(month));
        });
        server.get("/reports/rate-plans", ctx -> {
            YearMonth month = readMonth(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(ratePlanReport.generate(month));
        });
    }

    private Reservation find(long id) {
        return store.find(id).orElseThrow(() -> new HotelException(
                ErrorCode.RESERVATION_NOT_FOUND, "reservation " + id + " does not exist"));
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber()).orElseThrow();
    }

    private void update(long id, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static HotelException notAllowed(Reservation reservation, String operation) {
        return new HotelException(ErrorCode.OPERATION_NOT_ALLOWED,
                "cannot " + operation + " reservation " + reservation.id() + " while it is " + reservation.status());
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
'''

routes = Solution("architecture-routes")
routes.apply("09-pattern")
routes.write(HTTP_API, ROUTES_API)
routes.remove(SERVICE)
routes.remove(SERVICE_TEST)
routes.remove("src/test/java/hotel/reports/ReportsTest.java")
routes.replace(HOTEL, "import hotel.reservations.PriceCalculator;\nimport hotel.reservations.ReservationService;\n", "")
routes.replace(HOTEL, """        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store),
                new TourismTaxReport(store), new RatePlanReport(store));""",
               """        HttpApi api = new HttpApi(dataSource, rooms, store, new EmailSender(sentMessages), sentMessages,
                new OccupancyReport(rooms, store), new RevenueReport(store), new TourismTaxReport(store),
                new RatePlanReport(store));""")
routes.build_and_save()
