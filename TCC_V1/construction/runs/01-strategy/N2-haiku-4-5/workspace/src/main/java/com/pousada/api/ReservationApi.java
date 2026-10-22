package com.pousada.api;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.MessageService;
import com.pousada.service.ReservationService;
import com.pousada.utils.ComprovantePrinter;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationApi {
    private static final Logger log = LoggerFactory.getLogger(ReservationApi.class);
    private static final ReservationRepository reservationRepo = new ReservationRepository();
    private static final RoomRepository roomRepo = new RoomRepository();
    private static final MessageRepository messageRepo = new MessageRepository();
    private static final MessageService messageService = new MessageService(messageRepo);
    private static final ReservationService reservationService = new ReservationService(reservationRepo, roomRepo, messageService);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                JSONObject body = new JSONObject(ctx.body());

                // Validate required fields
                if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                    !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                    !body.has("guests") || !body.has("ratePlan")) {
                    ctx.status(400).json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "Missing required fields"));
                    return;
                }

                int roomNumber = body.getInt("roomNumber");
                String guestName = body.getString("guestName");
                String guestEmail = body.getString("guestEmail");
                String guestCpf = body.getString("guestCpf");
                String checkInStr = body.getString("checkIn");
                String checkOutStr = body.getString("checkOut");
                int guests = body.getInt("guests");
                String ratePlan = body.getString("ratePlan");
                String companyCode = body.optString("companyCode", null);

                LocalDate checkIn = LocalDate.parse(checkInStr, DATE_FORMAT);
                LocalDate checkOut = LocalDate.parse(checkOutStr, DATE_FORMAT);

                Reservation reservation = reservationService.create(roomNumber, guestName, guestEmail, guestCpf,
                                                                   checkIn, checkOut, guests, ratePlan, companyCode);

                ctx.status(201).json(reservationToJson(reservation));
            } catch (ReservationService.ApiException e) {
                ctx.status(getStatusCode(e.getCode()))
                    .json(new JSONObject()
                        .put("error", e.getCode())
                        .put("message", e.getMessage()));
            } catch (Exception e) {
                log.error("Error creating reservation", e);
                ctx.status(400).json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepo.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new JSONObject()
                        .put("error", "RESERVATION_NOT_FOUND")
                        .put("message", "Reservation not found"));
                    return;
                }

                ctx.json(reservationToJson(reservation));
            } catch (Exception e) {
                log.error("Error retrieving reservation", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                JSONObject body = new JSONObject(ctx.body());

                if (!body.has("amountPaid")) {
                    ctx.status(400).json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "Missing amountPaid"));
                    return;
                }

                BigDecimal amountPaid = BigDecimal.valueOf(body.getDouble("amountPaid"));
                Reservation reservation = reservationService.confirm(id, amountPaid);

                ctx.json(reservationToJson(reservation));
            } catch (ReservationService.ApiException e) {
                ctx.status(getStatusCode(e.getCode()))
                    .json(new JSONObject()
                        .put("error", e.getCode())
                        .put("message", e.getMessage()));
            } catch (Exception e) {
                log.error("Error confirming reservation", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancel(id);

                ctx.json(reservationToJson(reservation));
            } catch (ReservationService.ApiException e) {
                ctx.status(getStatusCode(e.getCode()))
                    .json(new JSONObject()
                        .put("error", e.getCode())
                        .put("message", e.getMessage()));
            } catch (Exception e) {
                log.error("Error cancelling reservation", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);

                ctx.json(reservationToJson(reservation));
            } catch (ReservationService.ApiException e) {
                ctx.status(getStatusCode(e.getCode()))
                    .json(new JSONObject()
                        .put("error", e.getCode())
                        .put("message", e.getMessage()));
            } catch (Exception e) {
                log.error("Error checking in reservation", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);

                Reservation finalRes = reservationRepo.findById(id);
                Room room = roomRepo.findById(finalRes.getRoomNumber());
                String comprovante = ComprovantePrinter.generate(finalRes, room.getType().name());

                JSONObject response = new JSONObject();
                response.put("title", "Comprovante de reserva");
                response.put("reservationId", finalRes.getId());
                response.put("guestName", finalRes.getGuestName());
                response.put("guestCpf", finalRes.getGuestCpf());
                response.put("roomNumber", finalRes.getRoomNumber());
                response.put("roomType", room.getType().name());
                response.put("checkIn", finalRes.getCheckIn());
                response.put("checkOut", finalRes.getCheckOut());
                response.put("nights", finalRes.getNights());
                response.put("subtotal", toDecimal(finalRes.getSubtotal()));
                response.put("discount", toDecimal(finalRes.getDiscount()));
                response.put("total", toDecimal(finalRes.getTotal()));

                ctx.json(response);
            } catch (ReservationService.ApiException e) {
                ctx.status(getStatusCode(e.getCode()))
                    .json(new JSONObject()
                        .put("error", e.getCode())
                        .put("message", e.getMessage()));
            } catch (Exception e) {
                log.error("Error checking out reservation", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepo.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new JSONObject()
                        .put("error", "RESERVATION_NOT_FOUND")
                        .put("message", "Reservation not found"));
                    return;
                }

                if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
                    ctx.status(400).json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "No paid reservation to print"));
                    return;
                }

                Room room = roomRepo.findById(reservation.getRoomNumber());

                JSONObject response = new JSONObject();
                response.put("title", "Comprovante de reserva");
                response.put("reservationId", reservation.getId());
                response.put("guestName", reservation.getGuestName());
                response.put("guestCpf", reservation.getGuestCpf());
                response.put("roomNumber", reservation.getRoomNumber());
                response.put("roomType", room.getType().name());
                response.put("checkIn", reservation.getCheckIn());
                response.put("checkOut", reservation.getCheckOut());
                response.put("nights", reservation.getNights());
                response.put("subtotal", toDecimal(reservation.getSubtotal()));
                response.put("discount", toDecimal(reservation.getDiscount()));
                response.put("total", toDecimal(reservation.getTotal()));

                ctx.json(response);
            } catch (Exception e) {
                log.error("Error retrieving document", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });
    }

    private static JSONObject reservationToJson(Reservation res) {
        JSONObject json = new JSONObject();
        json.put("id", res.getId());
        json.put("roomNumber", res.getRoomNumber());
        json.put("guestName", res.getGuestName());
        json.put("guestEmail", res.getGuestEmail());
        json.put("guestCpf", res.getGuestCpf());
        json.put("checkIn", res.getCheckIn());
        json.put("checkOut", res.getCheckOut());
        json.put("guests", res.getGuests());
        json.put("ratePlan", res.getRatePlan());
        json.put("nights", res.getNights());
        json.put("subtotal", toDecimal(res.getSubtotal()));
        json.put("discount", toDecimal(res.getDiscount()));
        json.put("total", toDecimal(res.getTotal()));
        json.put("status", res.getStatus().name());
        json.put("amountPaid", toDecimal(res.getAmountPaid()));
        if (res.getRefund() != null) {
            json.put("refund", toDecimal(res.getRefund()));
        } else {
            json.put("refund", JSONObject.NULL);
        }
        return json;
    }

    private static double toDecimal(BigDecimal value) {
        return Double.parseDouble(String.format("%.2f", value));
    }

    private static int getStatusCode(String code) {
        return switch (code) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
    }
}
