package com.pousada.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.domain.*;
import com.pousada.service.MessageService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import com.pousada.util.ErrorResponse;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationHandler {
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;
    private static final ReservationService reservationService = new ReservationService();

    public static void createReservation(Context ctx) {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (!validateCreateRequest(body, ctx)) {
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), dateFormatter);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), dateFormatter);
            int guests = body.get("guests").getAsInt();
            RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").getAsString());
            String companyCode = body.has("companyCode") && !body.get("companyCode").isJsonNull() ?
                                body.get("companyCode").getAsString() : null;

            long reservationId = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode
            );

            Reservation res = reservationService.getReservation(reservationId);
            ctx.status(201);
            ctx.json(serializeReservation(res));

        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public static void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = reservationService.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(serializeReservation(res));

        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }

    public static void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (!body.has("amountPaid") || body.get("amountPaid").isJsonNull()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing amountPaid"));
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();

            reservationService.confirmReservation(id, amountPaid);

            Reservation res = reservationService.getReservation(id);
            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            sendConfirmationEmail(res);
            ctx.json(serializeReservation(res));

        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }

    public static void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.cancelReservation(id);

            Reservation res = reservationService.getReservation(id);
            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(serializeReservation(res));

        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }

    public static void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkIn(id);

            Reservation res = reservationService.getReservation(id);
            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(serializeReservation(res));

        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }

    public static void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkOut(id);

            Reservation res = reservationService.getReservation(id);
            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Room room = RoomService.getRoomByNumber(res.getRoomNumber());
            ReservationDocument doc = new ReservationDocument(
                res.getId(),
                res.getGuestName(),
                res.getGuestCpf(),
                res.getRoomNumber(),
                room.getType().toString(),
                res.getCheckIn(),
                res.getCheckOut(),
                res.getNights(),
                res.getSubtotal(),
                res.getDiscount(),
                res.getTotal()
            );

            ctx.json(doc);

        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }

    public static void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = reservationService.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (res.getAmountPaid() == null || res.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not found"));
                return;
            }

            Room room = RoomService.getRoomByNumber(res.getRoomNumber());
            ReservationDocument doc = new ReservationDocument(
                res.getId(),
                res.getGuestName(),
                res.getGuestCpf(),
                res.getRoomNumber(),
                room.getType().toString(),
                res.getCheckIn(),
                res.getCheckOut(),
                res.getNights(),
                res.getSubtotal(),
                res.getDiscount(),
                res.getTotal()
            );

            ctx.json(doc);

        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }

    private static void sendConfirmationEmail(Reservation res) throws Exception {
        Room room = RoomService.getRoomByNumber(res.getRoomNumber());

        String receipt = formatReceipt(res, room);

        String emailBody = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            res.getGuestName(),
            receipt
        );

        MessageService.sendMessage("EMAIL", res.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);
    }

    private static String formatReceipt(Reservation res, Room room) {
        String nightsStr = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";
        LocalDate checkIn = res.getCheckIn();
        LocalDate checkOut = res.getCheckOut();

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", res.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n", res.getGuestName(), formatCpf(res.getGuestCpf())));
        receipt.append(String.format("Quarto: %d (%s)\n", res.getRoomNumber(), room.getType()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s\n",
            formatDate(checkIn), formatDate(checkOut), nightsStr));
        receipt.append(String.format("Diárias: R$ %s\n", formatMoney(res.getSubtotal())));

        if (res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: R$ %s\n", formatMoney(res.getDiscount())));
        }

        receipt.append(String.format("Total: R$ %s", formatMoney(res.getTotal())));

        return receipt.toString();
    }

    private static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format("%.2f", amount).replace(".", ",");
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9)
        );
    }

    private static Object serializeReservation(Reservation res) {
        JsonObject json = new JsonObject();
        json.addProperty("id", res.getId());
        json.addProperty("roomNumber", res.getRoomNumber());
        json.addProperty("guestName", res.getGuestName());
        json.addProperty("guestEmail", res.getGuestEmail());
        json.addProperty("guestCpf", res.getGuestCpf());
        json.addProperty("checkIn", res.getCheckIn().toString());
        json.addProperty("checkOut", res.getCheckOut().toString());
        json.addProperty("guests", res.getGuests());
        json.addProperty("ratePlan", res.getRatePlan().toString());
        json.addProperty("nights", res.getNights());
        json.addProperty("subtotal", res.getSubtotal().doubleValue());
        json.addProperty("discount", res.getDiscount().doubleValue());
        json.addProperty("total", res.getTotal().doubleValue());
        json.addProperty("status", res.getStatus().toString());
        json.addProperty("amountPaid", res.getAmountPaid().doubleValue());
        if (res.getRefund() != null) {
            json.addProperty("refund", res.getRefund().doubleValue());
        } else {
            json.add("refund", null);
        }
        return json;
    }

    private static void handleError(Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND":
                ctx.status(404);
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409);
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is unavailable for the requested dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422);
                ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Too many guests for this room"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation state"));
                break;
            default:
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private static boolean validateCreateRequest(JsonObject body, Context ctx) {
        if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
            !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
            !body.has("guests") || !body.has("ratePlan")) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
            return false;
        }
        return true;
    }
}
