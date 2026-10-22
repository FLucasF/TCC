package com.pousada.api.handler;

import com.pousada.api.JsonUtil;
import com.pousada.domain.RatePlan;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import org.json.JSONObject;
import java.time.LocalDate;

public class ReservationHandler {
    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                JSONObject body = new JSONObject(ctx.body());

                // Validate identification fields
                String guestCpf = body.optString("guestCpf", null);
                String companyCnpj = body.optString("companyCnpj", null);
                String companyName = body.optString("companyName", null);
                String guestPassport = body.optString("guestPassport", null);
                String guestCountry = body.optString("guestCountry", null);

                int identificationCount = 0;
                if (guestCpf != null && !guestCpf.isEmpty()) identificationCount++;
                if (companyCnpj != null && !companyCnpj.isEmpty()) identificationCount++;
                if (guestPassport != null && !guestPassport.isEmpty()) identificationCount++;

                if (identificationCount != 1) {
                    ctx.status(400);
                    ctx.json(JsonUtil.error("INVALID_REQUEST", "Exactly one identification type is required"));
                    return;
                }

                // Validate that CNPJ and CNPJ company name are together
                if ((companyCnpj != null && !companyCnpj.isEmpty()) != (companyName != null && !companyName.isEmpty())) {
                    ctx.status(400);
                    ctx.json(JsonUtil.error("INVALID_REQUEST", "Company CNPJ and name must be provided together"));
                    return;
                }

                // Validate that passport and country are together
                if ((guestPassport != null && !guestPassport.isEmpty()) != (guestCountry != null && !guestCountry.isEmpty())) {
                    ctx.status(400);
                    ctx.json(JsonUtil.error("INVALID_REQUEST", "Passport and country must be provided together"));
                    return;
                }

                int roomNumber = body.getInt("roomNumber");
                String guestName = body.getString("guestName");
                String guestEmail = body.getString("guestEmail");
                LocalDate checkIn = LocalDate.parse(body.getString("checkIn"));
                LocalDate checkOut = LocalDate.parse(body.getString("checkOut"));
                int guests = body.getInt("guests");
                RatePlan ratePlan = RatePlan.valueOf(body.getString("ratePlan"));

                var reservation = ReservationService.getInstance().createReservation(
                    roomNumber, guestName, guestEmail,
                    guestCpf != null && !guestCpf.isEmpty() ? guestCpf : null,
                    companyCnpj != null && !companyCnpj.isEmpty() ? companyCnpj : null,
                    companyName != null && !companyName.isEmpty() ? companyName : null,
                    guestPassport != null && !guestPassport.isEmpty() ? guestPassport : null,
                    guestCountry != null && !guestCountry.isEmpty() ? guestCountry : null,
                    checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201);
                ctx.json(JsonUtil.reservationToJson(reservation).toMap());
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(JsonUtil.error("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.getInstance().getReservation(id);
                ctx.json(JsonUtil.reservationToJson(reservation).toMap());
            } catch (IllegalArgumentException e) {
                ctx.status(404);
                ctx.json(JsonUtil.error("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.getInstance().confirmReservation(id);
                ctx.json(JsonUtil.reservationToJson(reservation).toMap());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404);
                    ctx.json(JsonUtil.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409);
                    ctx.json(JsonUtil.error("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.getInstance().checkIn(id);
                ctx.json(JsonUtil.reservationToJson(reservation).toMap());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404);
                    ctx.json(JsonUtil.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409);
                    ctx.json(JsonUtil.error("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = ReservationService.getInstance().checkOut(id);
                ctx.json(JsonUtil.documentToJson(document).toMap());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404);
                    ctx.json(JsonUtil.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409);
                    ctx.json(JsonUtil.error("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.getInstance().cancelReservation(id);
                ctx.json(JsonUtil.reservationToJson(reservation).toMap());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404);
                    ctx.json(JsonUtil.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409);
                    ctx.json(JsonUtil.error("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = ReservationService.getInstance().getDocument(id);
                ctx.json(JsonUtil.documentToJson(document).toMap());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404);
                    ctx.json(JsonUtil.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409);
                    ctx.json(JsonUtil.error("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            }
        });
    }

    private static void handleError(io.javalin.http.Context ctx, String message) {
        if (message.contains("Room not found")) {
            ctx.status(404);
            ctx.json(JsonUtil.error("ROOM_NOT_FOUND", message));
        } else if (message.contains("Room unavailable")) {
            ctx.status(409);
            ctx.json(JsonUtil.error("ROOM_UNAVAILABLE", message));
        } else if (message.contains("Invalid number of guests")) {
            ctx.status(422);
            ctx.json(JsonUtil.error("CAPACITY_EXCEEDED", message));
        } else if (message.contains("Invalid dates")) {
            ctx.status(400);
            ctx.json(JsonUtil.error("INVALID_REQUEST", message));
        } else {
            ctx.status(400);
            ctx.json(JsonUtil.error("INVALID_REQUEST", message));
        }
    }
}
