package com.pousada.marealta;

import com.pousada.marealta.api.ApiResponse;
import com.pousada.marealta.db.Database;
import com.pousada.marealta.models.*;
import com.pousada.marealta.services.EmailService;
import com.pousada.marealta.services.ReservationService;
import com.pousada.marealta.util.PricingUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Main {
    private static Database database;
    private static ReservationService reservationService;
    private static EmailService emailService;

    public static void main(String[] args) throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        emailService = new EmailService(database, testMode);
        reservationService = new ReservationService(database, emailService);

        Javalin app = Javalin.create().start(port);

        app.get("/rooms", Main::getRooms);
        app.post("/reservations", Main::createReservation);
        app.get("/reservations/{id}", Main::getReservation);
        app.post("/reservations/{id}/confirmation", Main::confirmReservation);
        app.post("/reservations/{id}/cancellation", Main::cancelReservation);
        app.post("/reservations/{id}/check-in", Main::checkIn);
        app.post("/reservations/{id}/check-out", Main::checkOut);
        app.get("/reservations/{id}/document", Main::getDocument);
        app.get("/messages", Main::getMessages);
        app.get("/reports/occupancy", Main::getOccupancyReport);
        app.get("/reports/revenue", Main::getRevenueReport);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ApiResponse("INTERNAL_ERROR", e.getMessage()));
        });
    }

    private static void getRooms(Context ctx) throws SQLException {
        List<Room> rooms = database.getAllRooms();
        ctx.json(rooms);
    }

    private static void createReservation(Context ctx) throws SQLException {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            int roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            int guests = ((Number) body.get("guests")).intValue();
            String ratePlan = (String) body.get("ratePlan");

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);

            Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

            ctx.status(201);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void getReservation(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = database.getReservation(id);
            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ApiResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } else {
                ctx.json(reservation);
            }
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private static void confirmReservation(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void cancelReservation(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void checkIn(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void checkOut(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Receipt receipt = reservationService.checkOut(id);
            ctx.json(receipt);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void getDocument(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Receipt receipt = reservationService.getReceipt(id);
            ctx.json(receipt);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void getMessages(Context ctx) throws SQLException {
        List<Message> messages = database.getAllMessages();
        ctx.json(messages);
    }

    private static void getOccupancyReport(Context ctx) throws SQLException {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ApiResponse("INVALID_REQUEST", "date parameter required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            List<String[]> report = database.getOccupancyReport(date);

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");
            for (String[] row : report) {
                csv.append(String.join(",", row)).append("\n");
            }

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void getRevenueReport(Context ctx) throws SQLException {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ApiResponse("INVALID_REQUEST", "month parameter required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            List<String[]> report = database.getRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");
            for (String[] row : report) {
                csv.append(String.join(",", row)).append("\n");
            }

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ApiResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static void handleError(Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND":
                ctx.status(404);
                ctx.json(new ApiResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404);
                ctx.json(new ApiResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409);
                ctx.json(new ApiResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422);
                ctx.json(new ApiResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409);
                ctx.json(new ApiResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation status"));
                break;
            case "INVALID_REQUEST":
                ctx.status(400);
                ctx.json(new ApiResponse("INVALID_REQUEST", "Invalid request"));
                break;
            default:
                ctx.status(400);
                ctx.json(new ApiResponse("INVALID_REQUEST", "Invalid request"));
        }
    }
}
