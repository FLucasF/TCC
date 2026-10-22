package com.pousada.api;

import io.javalin.Javalin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.db.Database;
import com.pousada.model.Document;
import com.pousada.model.Reservation;
import com.pousada.service.ReservationService;
import java.time.LocalDate;
import java.util.Map;

public class ReservationController {
    private static final Gson gson = new Gson();

    public static void setup(Javalin app, Database db) {
        ReservationService service = new ReservationService(db);

        app.post("/reservations", ctx -> {
            try {
                JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

                validateReservationInput(body);

                int roomNumber = body.get("roomNumber").getAsInt();
                String guestName = body.get("guestName").getAsString();
                String guestEmail = body.get("guestEmail").getAsString();
                LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString());
                LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString());
                int guests = body.get("guests").getAsInt();
                String ratePlan = body.get("ratePlan").getAsString();

                String guestCpf = body.has("guestCpf") && !body.get("guestCpf").isJsonNull()
                    ? body.get("guestCpf").getAsString() : null;
                String companyCnpj = body.has("companyCnpj") && !body.get("companyCnpj").isJsonNull()
                    ? body.get("companyCnpj").getAsString() : null;
                String companyName = body.has("companyName") && !body.get("companyName").isJsonNull()
                    ? body.get("companyName").getAsString() : null;
                String guestPassport = body.has("guestPassport") && !body.get("guestPassport").isJsonNull()
                    ? body.get("guestPassport").getAsString() : null;
                String guestCountry = body.has("guestCountry") && !body.get("guestCountry").isJsonNull()
                    ? body.get("guestCountry").getAsString() : null;

                int identificationCount = (guestCpf != null ? 1 : 0) +
                                         ((companyCnpj != null) ? 1 : 0) +
                                         ((guestPassport != null) ? 1 : 0);

                if (identificationCount != 1) {
                    ctx.status(400).json(Map.of(
                        "error", "INVALID_REQUEST",
                        "message", "Exatamente um tipo de identificação deve ser fornecido"
                    ));
                    return;
                }

                Reservation res = service.createReservation(roomNumber, guestName, guestEmail,
                        guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
                        checkIn, checkOut, guests, ratePlan);

                ctx.status(201).json(reservationToJson(res));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            } catch (Exception e) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", e.getMessage()
                ));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation res = service.getReservation(id);
                ctx.json(reservationToJson(res));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation res = service.confirmReservation(id);
                ctx.json(reservationToJson(res));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation res = service.cancelReservation(id);
                ctx.json(reservationToJson(res));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation res = service.checkIn(id);
                ctx.json(reservationToJson(res));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Document doc = service.checkOut(id);
                ctx.json(documentToJson(doc));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Document doc = service.getDocument(id);
                ctx.json(documentToJson(doc));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e);
            }
        });
    }

    private static void validateReservationInput(JsonObject body) throws Exception {
        String[] required = {"roomNumber", "guestName", "guestEmail", "checkIn", "checkOut", "guests", "ratePlan"};
        for (String field : required) {
            if (!body.has(field) || body.get(field).isJsonNull()) {
                throw new Exception("Campo obrigatório faltando: " + field);
            }
        }
    }

    private static Map<String, Object> reservationToJson(Reservation res) {
        return Map.ofEntries(
            Map.entry("id", res.id),
            Map.entry("roomNumber", res.roomNumber),
            Map.entry("guestName", res.guestName),
            Map.entry("guestEmail", res.guestEmail),
            Map.entry("guestCpf", res.guestCpf),
            Map.entry("companyCnpj", res.companyCnpj),
            Map.entry("companyName", res.companyName),
            Map.entry("guestPassport", res.guestPassport),
            Map.entry("guestCountry", res.guestCountry),
            Map.entry("checkIn", res.checkIn.toString()),
            Map.entry("checkOut", res.checkOut.toString()),
            Map.entry("guests", res.guests),
            Map.entry("ratePlan", res.ratePlan),
            Map.entry("nights", res.nights),
            Map.entry("subtotal", res.subtotal),
            Map.entry("discount", res.discount),
            Map.entry("total", res.total),
            Map.entry("status", res.status),
            Map.entry("amountPaid", res.amountPaid),
            Map.entry("refund", res.refund)
        );
    }

    private static Map<String, Object> documentToJson(Document doc) {
        return Map.ofEntries(
            Map.entry("title", doc.title),
            Map.entry("reservationId", doc.reservationId),
            Map.entry("guestName", doc.guestName),
            Map.entry("guestCpf", doc.guestCpf),
            Map.entry("companyCnpj", doc.companyCnpj),
            Map.entry("companyName", doc.companyName),
            Map.entry("guestPassport", doc.guestPassport),
            Map.entry("guestCountry", doc.guestCountry),
            Map.entry("roomNumber", doc.roomNumber),
            Map.entry("roomType", doc.roomType),
            Map.entry("checkIn", doc.checkIn.toString()),
            Map.entry("checkOut", doc.checkOut.toString()),
            Map.entry("nights", doc.nights),
            Map.entry("subtotal", doc.subtotal),
            Map.entry("discount", doc.discount),
            Map.entry("total", doc.total),
            Map.entry("text", doc.text)
        );
    }

    private static void handleValidationError(io.javalin.http.Context ctx, ReservationService.ValidationException e) {
        int status = switch (e.code) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
        ctx.status(status).json(Map.of("error", e.code, "message", e.message));
    }
}
