package com.pousadamarealta.routes;

import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.util.DateFormatter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReservationRoutes {
    private final ReservationService reservationService = new ReservationService();
    private final ObjectMapper mapper = new ObjectMapper();

    public void createReservation(Context ctx) {
        try {
            var body = ctx.bodyAsClass(Map.class);

            // Validate required fields
            String[] required = {"roomNumber", "guestName", "guestEmail", "guestCpf", "checkIn", "checkOut", "guests", "ratePlan"};
            for (String field : required) {
                if (!body.containsKey(field)) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required field: " + field));
                    return;
                }
            }

            int roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            LocalDate checkIn = DateFormatter.parse((String) body.get("checkIn"));
            LocalDate checkOut = DateFormatter.parse((String) body.get("checkOut"));
            int guests = ((Number) body.get("guests")).intValue();
            String ratePlan = (String) body.get("ratePlan");

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(reservationToJson(reservation));
        } catch (ReservationService.ValidationException e) {
            if ("ROOM_NOT_FOUND".equals(e.errorCode)) {
                ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else if ("ROOM_UNAVAILABLE".equals(e.errorCode)) {
                ctx.status(409).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else if ("CAPACITY_EXCEEDED".equals(e.errorCode)) {
                ctx.status(422).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else {
                ctx.status(400).json(new ErrorResponse(e.errorCode, e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ValidationException e) {
            ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ValidationException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.errorCode)) {
                ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else {
                ctx.status(409).json(new ErrorResponse(e.errorCode, e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ValidationException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.errorCode)) {
                ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else {
                ctx.status(409).json(new ErrorResponse(e.errorCode, e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkInReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ValidationException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.errorCode)) {
                ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else {
                ctx.status(409).json(new ErrorResponse(e.errorCode, e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOutReservation(id);
            var document = reservationService.getDocument(id);

            Map<String, Object> response = new HashMap<>();
            response.put("title", document.title);
            response.put("reservationId", document.reservationId);
            response.put("guestName", document.guestName);
            response.put("guestCpf", document.guestCpf);
            response.put("roomNumber", document.roomNumber);
            response.put("roomType", document.roomType);
            response.put("checkIn", DateFormatter.formatISO(document.checkIn));
            response.put("checkOut", DateFormatter.formatISO(document.checkOut));
            response.put("nights", document.nights);
            response.put("subtotal", document.subtotal.doubleValue());
            response.put("discount", document.discount.doubleValue());
            response.put("total", document.total.doubleValue());

            ctx.json(response);
        } catch (ReservationService.ValidationException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.errorCode)) {
                ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else {
                ctx.status(409).json(new ErrorResponse(e.errorCode, e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var document = reservationService.getDocument(id);

            Map<String, Object> response = new HashMap<>();
            response.put("title", document.title);
            response.put("reservationId", document.reservationId);
            response.put("guestName", document.guestName);
            response.put("guestCpf", document.guestCpf);
            response.put("roomNumber", document.roomNumber);
            response.put("roomType", document.roomType);
            response.put("checkIn", DateFormatter.formatISO(document.checkIn));
            response.put("checkOut", DateFormatter.formatISO(document.checkOut));
            response.put("nights", document.nights);
            response.put("subtotal", document.subtotal.doubleValue());
            response.put("discount", document.discount.doubleValue());
            response.put("total", document.total.doubleValue());

            ctx.json(response);
        } catch (ReservationService.ValidationException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.errorCode)) {
                ctx.status(404).json(new ErrorResponse(e.errorCode, e.getMessage()));
            } else {
                ctx.status(409).json(new ErrorResponse(e.errorCode, e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private Map<String, Object> reservationToJson(Reservation res) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", res.id);
        map.put("roomNumber", res.roomNumber);
        map.put("guestName", res.guestName);
        map.put("guestEmail", res.guestEmail);
        map.put("guestCpf", res.guestCpf);
        map.put("checkIn", DateFormatter.formatISO(res.checkIn));
        map.put("checkOut", DateFormatter.formatISO(res.checkOut));
        map.put("guests", res.guests);
        map.put("ratePlan", res.ratePlan);
        map.put("nights", res.nights);
        map.put("subtotal", res.subtotal.doubleValue());
        map.put("discount", res.discount.doubleValue());
        map.put("total", res.total.doubleValue());
        map.put("status", res.status);
        map.put("amountPaid", res.amountPaid.doubleValue());
        map.put("refund", res.refund != null ? res.refund.doubleValue() : null);
        return map;
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
