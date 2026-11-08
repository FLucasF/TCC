package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pousada.db.Database;
import com.pousada.dto.*;
import com.pousada.model.*;
import com.pousada.service.DocumentGenerator;
import com.pousada.service.ReportGenerator;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class App {
    private static Database database;
    private static ReservationService reservationService;
    private static ReportGenerator reportGenerator;
    private static ObjectMapper objectMapper;

    public static void main(String[] args) throws Exception {
        initializeDatabase();
        initializeServices();
        setupApp();
    }

    private static void initializeDatabase() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Missing database configuration");
        }

        database = new Database(dbUrl, dbUser, dbPassword);
    }

    private static void initializeServices() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        reservationService = new ReservationService(database);
        reportGenerator = new ReportGenerator(database);
    }

    private static void setupApp() throws Exception {
        int port = 7070;
        if (System.getenv("PORT") != null) {
            port = Integer.parseInt(System.getenv("PORT"));
        }

        Javalin app = Javalin.create();
        app.start(port);

        // GET /rooms
        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = database.getAllRooms();
                List<RoomResponse> responses = new ArrayList<>();

                for (Room room : rooms) {
                    RoomResponse response = new RoomResponse();
                    response.number = room.getNumber();
                    response.type = room.getType().getDisplayName();
                    response.capacity = room.getCapacity();
                    response.nightlyRate = room.getNightlyRate();
                    responses.add(response);
                }

                ctx.json(responses);
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

                if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null ||
                    req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
                    req.ratePlan == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                LocalDate checkIn = LocalDate.parse(req.checkIn, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate checkOut = LocalDate.parse(req.checkOut, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                RatePlan ratePlan = RatePlan.valueOf(req.ratePlan);

                Reservation reservation = reservationService.createReservation(
                    req.roomNumber, req.guestName, req.guestEmail, req.guestCpf,
                    checkIn, checkOut, req.guests, ratePlan
                );

                ctx.status(201);
                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Room not found"));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422);
                ctx.json(new ErrorResponse(e.code, "Capacity exceeded"));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409);
                ctx.json(new ErrorResponse(e.code, "Room unavailable"));
            } catch (ReservationService.InvalidRequestException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse(e.code, "Invalid request"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/{id}", ctx -> {
            try {
                long reservationId = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(reservationId);
                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Reservation not found"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long reservationId = Long.parseLong(ctx.pathParam("id"));
                ConfirmationRequest req = ctx.bodyAsClass(ConfirmationRequest.class);

                Reservation reservation = reservationService.confirmReservation(reservationId, req.amountPaid);

                Room room = database.getRoomByNumber(reservation.getRoomNumber());
                String roomType = room.getType().getDisplayName();

                String emailBody = DocumentGenerator.generateConfirmationEmail(reservation, roomType);
                database.saveMessage(MessageChannel.EMAIL, reservation.getGuestEmail(),
                    "Reserva confirmada - Pousada Maré Alta", emailBody);

                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409);
                ctx.json(new ErrorResponse(e.code, "Operation not allowed"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long reservationId = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(reservationId);
                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409);
                ctx.json(new ErrorResponse(e.code, "Operation not allowed"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long reservationId = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkInReservation(reservationId);
                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409);
                ctx.json(new ErrorResponse(e.code, "Operation not allowed"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long reservationId = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOutReservation(reservationId);

                Room room = database.getRoomByNumber(reservation.getRoomNumber());
                String roomType = room.getType().getDisplayName();

                DocumentResponse doc = new DocumentResponse();
                doc.reservationId = reservation.getId();
                doc.guestName = reservation.getGuestName();
                doc.guestCpf = reservation.getGuestCpf();
                doc.roomNumber = reservation.getRoomNumber();
                doc.roomType = roomType;
                doc.checkIn = reservation.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                doc.checkOut = reservation.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                doc.nights = reservation.getNights();
                doc.subtotal = reservation.getSubtotal();
                doc.discount = reservation.getDiscount();
                doc.total = reservation.getTotal();

                ctx.json(doc);
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409);
                ctx.json(new ErrorResponse(e.code, "Operation not allowed"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/{id}/document", ctx -> {
            try {
                long reservationId = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(reservationId);

                if (reservation.getStatus() == ReservationStatus.PENDING) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document not available for pending reservations"));
                    return;
                }

                Room room = database.getRoomByNumber(reservation.getRoomNumber());
                String roomType = room.getType().getDisplayName();

                DocumentResponse doc = new DocumentResponse();
                doc.reservationId = reservation.getId();
                doc.guestName = reservation.getGuestName();
                doc.guestCpf = reservation.getGuestCpf();
                doc.roomNumber = reservation.getRoomNumber();
                doc.roomType = roomType;
                doc.checkIn = reservation.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                doc.checkOut = reservation.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                doc.nights = reservation.getNights();
                doc.subtotal = reservation.getSubtotal();
                doc.discount = reservation.getDiscount();
                doc.total = reservation.getTotal();

                ctx.json(doc);
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, "Reservation not found"));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            try {
                List<Message> messages = database.getAllMessages();
                List<MessageResponse> responses = new ArrayList<>();

                for (Message message : messages) {
                    MessageResponse response = new MessageResponse();
                    response.id = message.getId();
                    response.channel = message.getChannel().name();
                    response.to = message.getTo();
                    response.subject = message.getSubject();
                    response.body = message.getBody();
                    responses.add(response);
                }

                ctx.json(responses);
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String report = reportGenerator.generateOccupancyReport(date);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportGenerator.generateRevenueReport(month);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reports/tourism-tax
        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportGenerator.generateTourismTaxReport(month);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reports/rate-plans
        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportGenerator.generateRatePlanReport(month);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });
    }

    private static ReservationResponse toReservationResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        response.checkOut = reservation.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().name();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus().name();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();
        return response;
    }
}
