package com.pousadamareaita.http;

import com.pousadamareaita.exception.ReservationException;
import com.pousadamareaita.http.dto.ErrorDto;
import com.pousadamareaita.http.dto.ReceiptDto;
import com.pousadamareaita.http.dto.ReservationDto;
import com.pousadamareaita.model.RatePlan;
import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.Room;
import com.pousadamareaita.service.ReservationService;
import com.pousadamareaita.storage.RoomRepository;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
        this.roomRepository = new RoomRepository();
    }

    public void registerRoutes(Javalin app) {
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            int roomNumber = getIntField(body, "roomNumber");
            String guestName = getStringField(body, "guestName");
            String guestEmail = getStringField(body, "guestEmail");
            String guestCpf = getStringField(body, "guestCpf");
            LocalDate checkIn = getDateField(body, "checkIn");
            LocalDate checkOut = getDateField(body, "checkOut");
            int guests = getIntField(body, "guests");
            RatePlan ratePlan = getRatePlanField(body, "ratePlan");
            String companyCode = getOptionalStringField(body, "companyCode");

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode
            );

            ctx.status(201);
            ctx.json(new ReservationDto(reservation));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(new ReservationDto(reservation));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            BigDecimal amountPaid = getBigDecimalField(body, "amountPaid");

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(new ReservationDto(reservation));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(new ReservationDto(reservation));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void checkIn(io.javalin.http.Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(new ReservationDto(reservation));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void checkOut(io.javalin.http.Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            String roomType = room != null ? room.getType().toString() : "UNKNOWN";
            ctx.json(new ReceiptDto(reservation, roomType));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void getDocument(io.javalin.http.Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
                sendErrorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", "Document only available for paid reservations");
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            String roomType = room != null ? room.getType().toString() : "UNKNOWN";
            ctx.json(new ReceiptDto(reservation, roomType));
        } catch (ReservationException e) {
            sendErrorResponse(ctx, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void sendErrorResponse(io.javalin.http.Context ctx, int status, String errorCode, String message) {
        ctx.status(status);
        ctx.json(new ErrorDto(errorCode, message));
    }

    private int getIntField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        Object value = obj.get(field);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        throw new IllegalArgumentException("Invalid type for field: " + field);
    }

    private String getStringField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return (String) obj.get(field);
    }

    private String getOptionalStringField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field) || obj.get(field) == null) {
            return null;
        }
        return (String) obj.get(field);
    }

    private BigDecimal getBigDecimalField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        Object value = obj.get(field);
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("Invalid type for field: " + field);
    }

    private LocalDate getDateField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        try {
            return LocalDate.parse((String) obj.get(field), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format for " + field + ": " + e.getMessage());
        }
    }

    private RatePlan getRatePlanField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        try {
            return RatePlan.valueOf((String) obj.get(field));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid rate plan: " + e.getMessage());
        }
    }
}
