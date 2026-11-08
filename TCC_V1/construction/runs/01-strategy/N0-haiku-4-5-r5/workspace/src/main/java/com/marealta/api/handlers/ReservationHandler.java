package com.marealta.api.handlers;

import io.javalin.http.Context;
import com.marealta.service.ReservationService;
import com.marealta.service.MessageService;
import com.marealta.service.ReceiptService;
import com.marealta.service.RoomService;
import com.marealta.domain.Reservation;
import com.marealta.domain.Room;
import com.marealta.api.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationHandler {
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void create(Context ctx) throws Exception {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                !body.has("guests") || !body.has("ratePlan")) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), DATE_FORMAT);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), DATE_FORMAT);
            int guests = body.get("guests").getAsInt();
            String ratePlan = body.get("ratePlan").getAsString();
            String companyCode = body.has("companyCode") ? body.get("companyCode").getAsString() : null;

            Reservation res = ReservationService.create(roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode);

            ctx.status(201);
            ctx.json(toJson(res));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request format"));
        }
    }

    public static void get(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservationById(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(toJson(res));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public static void confirm(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservationById(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            BigDecimal amountPaid = res.total;
            ReservationService.confirm(id, amountPaid);

            Room room = RoomService.getRoomByNumber(res.roomNumber);
            String emailBody = ReceiptService.generateConfirmationEmail(res, room.type);
            MessageService.sendEmail(res.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);

            res = ReservationService.getReservationById(id);
            ctx.json(toJson(res));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm this reservation"));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public static void cancel(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservationById(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ReservationService.cancel(id, res.ratePlan);
            res = ReservationService.getReservationById(id);
            ctx.json(toJson(res));
        } catch (IllegalArgumentException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation"));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public static void checkIn(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservationById(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ReservationService.checkIn(id);
            res = ReservationService.getReservationById(id);
            ctx.json(toJson(res));
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check in this reservation"));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public static void checkOut(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservationById(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ReservationService.checkOut(id);
            Room room = RoomService.getRoomByNumber(res.roomNumber);

            var receiptJson = new Object() {
                public String title = "Comprovante de reserva";
                public int reservationId = res.id;
                public String guestName = res.guestName;
                public String guestCpf = res.guestCpf;
                public int roomNumber = res.roomNumber;
                public String roomType = room.type;
                public String checkIn = res.checkIn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                public String checkOut = res.checkOut.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                public int nights = res.nights;
                public double subtotal = res.subtotal.doubleValue();
                public double discount = res.discount.doubleValue();
                public double total = res.total.doubleValue();
            };

            ctx.json(receiptJson);
        } catch (IllegalStateException e) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check out this reservation"));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public static void getDocument(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservationById(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (res.amountPaid.compareTo(java.math.BigDecimal.ZERO) == 0) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "No document available"));
                return;
            }

            Room room = RoomService.getRoomByNumber(res.roomNumber);

            var receiptJson = new Object() {
                public String title = "Comprovante de reserva";
                public int reservationId = res.id;
                public String guestName = res.guestName;
                public String guestCpf = res.guestCpf;
                public int roomNumber = res.roomNumber;
                public String roomType = room.type;
                public String checkIn = res.checkIn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                public String checkOut = res.checkOut.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                public int nights = res.nights;
                public double subtotal = res.subtotal.doubleValue();
                public double discount = res.discount.doubleValue();
                public double total = res.total.doubleValue();
            };

            ctx.json(receiptJson);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private static Object toJson(Reservation res) {
        return new Object() {
            public int id = res.id;
            public int roomNumber = res.roomNumber;
            public String guestName = res.guestName;
            public String guestEmail = res.guestEmail;
            public String guestCpf = res.guestCpf;
            public String checkIn = res.checkIn.format(DATE_FORMAT);
            public String checkOut = res.checkOut.format(DATE_FORMAT);
            public int guests = res.guests;
            public String ratePlan = res.ratePlan;
            public int nights = res.nights;
            public double subtotal = res.subtotal.doubleValue();
            public double discount = res.discount.doubleValue();
            public double total = res.total.doubleValue();
            public String status = res.status;
            public double amountPaid = res.amountPaid.doubleValue();
            public Double refund = res.refund != null ? res.refund.doubleValue() : null;
        };
    }

    private static void handleError(Context ctx, String error) {
        if (error.equals("ROOM_NOT_FOUND")) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } else if (error.equals("CAPACITY_EXCEEDED")) {
            ctx.status(422);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Room capacity exceeded"));
        } else if (error.equals("ROOM_UNAVAILABLE")) {
            ctx.status(409);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
        } else {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }
}
