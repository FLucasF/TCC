package com.pousada.marealta.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Handler {
    protected static final Gson gson = new Gson();

    protected static void error(Context ctx, int status, String code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", code);
        error.addProperty("message", message);
        ctx.status(status).json(error);
    }

    protected static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse("Invalid date format: " + dateStr);
        }
    }

    protected static String getRequiredString(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new BadRequestResponse("Missing required field: " + field);
        }
        return json.get(field).getAsString();
    }

    protected static int getRequiredInt(JsonObject json, String field) {
        if (!json.has(field)) {
            throw new BadRequestResponse("Missing required field: " + field);
        }
        return json.get(field).getAsInt();
    }

    protected static double getRequiredDouble(JsonObject json, String field) {
        if (!json.has(field)) {
            throw new BadRequestResponse("Missing required field: " + field);
        }
        return json.get(field).getAsDouble();
    }

    protected static void handleException(Context ctx, Exception e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }

        if ("ROOM_NOT_FOUND".equals(message)) {
            error(ctx, 404, "ROOM_NOT_FOUND", "Room not found");
        } else if ("RESERVATION_NOT_FOUND".equals(message)) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
        } else if ("ROOM_UNAVAILABLE".equals(message)) {
            error(ctx, 409, "ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        } else if ("OPERATION_NOT_ALLOWED".equals(message)) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", "This operation is not allowed in the current reservation state");
        } else if ("CAPACITY_EXCEEDED".equals(message)) {
            error(ctx, 422, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        } else if ("INVALID_REQUEST".equals(message)) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request parameters");
        } else if (e instanceof BadRequestResponse) {
            error(ctx, 400, "INVALID_REQUEST", e.getMessage());
        } else {
            error(ctx, 500, "INTERNAL_ERROR", "Internal server error");
            e.printStackTrace();
        }
    }
}
