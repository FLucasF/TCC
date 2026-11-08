package com.pousada.marealta;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.marealta.api.ApiError;
import com.pousada.marealta.api.request.CreateReservationRequest;
import com.pousada.marealta.api.response.ReservationResponse;
import com.pousada.marealta.api.response.DocumentResponse;
import com.pousada.marealta.api.response.MessageResponse;
import com.pousada.marealta.db.Database;
import com.pousada.marealta.db.RoomRepository;
import com.pousada.marealta.db.ReservationRepository;
import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.enums.RatePlan;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Document;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.notification.NotificationManager;
import com.pousada.marealta.notification.notifiers.GuestNotifier;
import com.pousada.marealta.notification.notifiers.HousekeepingNotifier;
import com.pousada.marealta.notification.notifiers.FinanceNotifier;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.service.ReportService;
import com.pousada.marealta.util.DocumentGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class App {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
        }

        Database.initialize(dbUrl, dbUser, dbPassword);
        initializeNotifiers();

        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> {
            try {
                var rooms = RoomRepository.findAll();
                ctx.json(rooms.stream().map(r -> new Object() {
                    public int number = r.getNumber();
                    public String type = r.getType().displayName;
                    public int capacity = r.getCapacity();
                    public String nightlyRate = String.format("%.2f", r.getNightlyRate());
                }).toList());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = gson.fromJson(ctx.body(), CreateReservationRequest.class);

                if (req.roomNumber <= 0 || req.guestName == null || req.guestEmail == null ||
                    req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
                    req.guests <= 0 || req.ratePlan == null) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                LocalDate checkIn = LocalDate.parse(req.checkIn);
                LocalDate checkOut = LocalDate.parse(req.checkOut);
                RatePlan ratePlan = RatePlan.valueOf(req.ratePlan);

                Reservation reservation = ReservationService.createReservation(
                    req.roomNumber, req.guestName, req.guestEmail, req.guestCpf,
                    checkIn, checkOut, req.guests, ratePlan
                );

                ctx.status(201).json(new ReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                ctx.json(new ReservationResponse(reservation));
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                Reservation confirmed = ReservationService.confirmReservation(id, reservation.getTotal());
                ctx.json(new ReservationResponse(confirmed));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation cancelled = ReservationService.cancelReservation(id);
                ctx.json(new ReservationResponse(cancelled));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation checkedIn = ReservationService.checkIn(id);
                ctx.json(new ReservationResponse(checkedIn));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                ReservationService.checkOut(id);
                Room room = RoomRepository.findByNumber(reservation.getRoomNumber());
                Document doc = new Document(
                    reservation.getId(),
                    reservation.getGuestName(),
                    reservation.getGuestCpf(),
                    reservation.getRoomNumber(),
                    room.getType().displayName,
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal()
                );

                ctx.json(new DocumentResponse(doc));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Document not available"));
                    return;
                }

                Room room = RoomRepository.findByNumber(reservation.getRoomNumber());
                Document doc = new Document(
                    reservation.getId(),
                    reservation.getGuestName(),
                    reservation.getGuestCpf(),
                    reservation.getRoomNumber(),
                    room.getType().displayName,
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal()
                );

                ctx.json(new DocumentResponse(doc));
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            try {
                var messages = MessageRepository.findAll();
                ctx.json(messages.stream().map(MessageResponse::new).toList());
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = ReportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "month parameter is required"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                String csv = ReportService.generateRevenueReport(yearMonth);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.close();
        }));

        app.start(port);
    }

    private static void initializeNotifiers() {
        NotificationManager.registerNotifier(new GuestNotifier());
        NotificationManager.registerNotifier(new HousekeepingNotifier());
        NotificationManager.registerNotifier(new FinanceNotifier());
    }

    private static void handleError(io.javalin.http.Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(new ApiError("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(new ApiError("ROOM_UNAVAILABLE", "Room is not available for the requested dates"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(new ApiError("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(new ApiError("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                break;
            case "INVALID_REQUEST":
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid request"));
                break;
            default:
                ctx.status(400).json(new ApiError("INVALID_REQUEST", error));
        }
    }
}
