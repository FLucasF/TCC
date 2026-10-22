package br.com.marealta;

import br.com.marealta.domain.*;
import br.com.marealta.repository.MessageRepository;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.MessageService;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class Application {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initSchema();
        roomRepository.initData();
        reservationRepository.initSchema();
        messageRepository.initSchema();

        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll().stream()
                .map(RoomDTO::from)
                .toList();
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

                validateRequired(request.roomNumber(), "roomNumber");
                validateRequired(request.guestName(), "guestName");
                validateRequired(request.guestEmail(), "guestEmail");
                validateRequired(request.guestCpf(), "guestCpf");
                validateRequired(request.checkIn(), "checkIn");
                validateRequired(request.checkOut(), "checkOut");
                validateRequired(request.guests(), "guests");
                validateRequired(request.ratePlan(), "ratePlan");

                LocalDate checkIn = parseDate(request.checkIn(), "checkIn");
                LocalDate checkOut = parseDate(request.checkOut(), "checkOut");
                DateRange dateRange = new DateRange(checkIn, checkOut);

                Reservation reservation = reservationService.createReservation(
                    request.roomNumber(),
                    request.guestName(),
                    request.guestEmail(),
                    request.guestCpf(),
                    dateRange,
                    request.guests(),
                    request.ratePlan(),
                    request.companyCode()
                );

                ctx.status(201).json(ReservationDTO.from(reservation));

            } catch (Reservation.CapacityValidationException e) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                Long id = ctx.pathParamAsClass("id", Long.class).get();
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                Long id = ctx.pathParamAsClass("id", Long.class).get();
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                Long id = ctx.pathParamAsClass("id", Long.class).get();
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                Long id = ctx.pathParamAsClass("id", Long.class).get();
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                Long id = ctx.pathParamAsClass("id", Long.class).get();
                Reservation reservation = reservationService.checkOut(id);
                ctx.json(DocumentDTO.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                Long id = ctx.pathParamAsClass("id", Long.class).get();
                Reservation reservation = reservationService.getReservation(id);

                if (!reservation.getStatus().isPaid()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Document only available for paid reservations"));
                    return;
                }

                ctx.json(DocumentDTO.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            var messages = messageService.getAllMessages().stream()
                .map(MessageDTO::from)
                .toList();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isBlank()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                    return;
                }

                LocalDate date = parseDate(dateStr, "date");
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isBlank()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                    return;
                }

                YearMonth month = parseMonth(monthStr);
                String csv = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        });
    }

    private static DataSource createDataSource() {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String user = System.getenv().getOrDefault("DB_USER", "postgres");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }

    private static void validateRequired(Object value, String fieldName) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is required");
        }
    }

    private static LocalDate parseDate(String dateStr, String fieldName) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for '" + fieldName + "': expected yyyy-MM-dd");
        }
    }

    private static YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format: expected yyyy-MM");
        }
    }
}
