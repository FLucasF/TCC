package br.com.pousadamareal.api.handlers;

import br.com.pousadamareal.api.dto.DocumentDTO;
import br.com.pousadamareal.api.dto.ReservationDTO;
import br.com.pousadamareal.domain.Document;
import br.com.pousadamareal.domain.RatePlan;
import br.com.pousadamareal.domain.Reservation;
import br.com.pousadamareal.service.ReservationService;
import br.com.pousadamareal.util.DateUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationHandler {
    private final ReservationService reservationService;

    public ReservationHandler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) throws SQLException {
        try {
            JsonElement element = JsonParser.parseString(ctx.body());
            if (!element.isJsonObject()) {
                ctx.status(400);
                ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid request body"));
                return;
            }
            JsonObject body = element.getAsJsonObject();

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("checkIn") || !body.has("checkOut") || !body.has("guests") || !body.has("ratePlan")) {
                ctx.status(400);
                ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            LocalDate checkIn = DateUtils.fromISO(body.get("checkIn").getAsString());
            LocalDate checkOut = DateUtils.fromISO(body.get("checkOut").getAsString());
            int guests = body.get("guests").getAsInt();
            RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").getAsString());

            String guestCpf = null;
            String companyCnpj = null;
            String companyName = null;
            String guestPassport = null;
            String guestCountry = null;

            int identificationCount = 0;
            if (body.has("guestCpf") && !body.get("guestCpf").isJsonNull()) {
                guestCpf = body.get("guestCpf").getAsString();
                identificationCount++;
            }
            if (body.has("companyCnpj") && !body.get("companyCnpj").isJsonNull()) {
                companyCnpj = body.get("companyCnpj").getAsString();
                if (body.has("companyName")) {
                    companyName = body.get("companyName").getAsString();
                }
                identificationCount++;
            }
            if (body.has("guestPassport") && !body.get("guestPassport").isJsonNull()) {
                guestPassport = body.get("guestPassport").getAsString();
                if (body.has("guestCountry")) {
                    guestCountry = body.get("guestCountry").getAsString();
                }
                identificationCount++;
            }

            if (identificationCount != 1) {
                ctx.status(400);
                ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Must provide exactly one identification method"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
                    guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan);

            ctx.status(201);
            ctx.json(ReservationDTO.toJson(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid request body"));
        }
    }

    public void getReservation(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(ReservationDTO.errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(ReservationDTO.toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void confirmReservation(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationDTO.toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void checkIn(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationDTO.toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void checkOut(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.checkOut(id);
            ctx.json(DocumentDTO.toJson(document));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void cancelReservation(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationDTO.toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void getDocument(Context ctx) throws SQLException {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.getDocument(id);
            ctx.json(DocumentDTO.toJson(document));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private void handleError(Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND" -> {
                ctx.status(404);
                ctx.json(ReservationDTO.errorResponse("ROOM_NOT_FOUND", "Room not found"));
            }
            case "RESERVATION_NOT_FOUND" -> {
                ctx.status(404);
                ctx.json(ReservationDTO.errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
            case "ROOM_UNAVAILABLE" -> {
                ctx.status(409);
                ctx.json(ReservationDTO.errorResponse("ROOM_UNAVAILABLE", "Room is not available for the specified dates"));
            }
            case "CAPACITY_EXCEEDED" -> {
                ctx.status(422);
                ctx.json(ReservationDTO.errorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
            }
            case "OPERATION_NOT_ALLOWED" -> {
                ctx.status(409);
                ctx.json(ReservationDTO.errorResponse("OPERATION_NOT_ALLOWED", "This operation is not allowed for the current reservation state"));
            }
            default -> {
                ctx.status(400);
                ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid request"));
            }
        }
    }
}
