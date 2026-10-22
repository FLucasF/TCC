package com.pousada.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pousada.business.ApiException;
import com.pousada.business.ReservationService;
import com.pousada.db.*;
import com.pousada.models.*;
import com.pousada.reports.*;
import com.pousada.utils.PricingUtils;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApiHandlers {
    private final Gson gson = new GsonBuilder().create();
    private final RoomRepository roomRepo;
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepo;
    private final MessageRepository messageRepo;

    public ApiHandlers(RoomRepository roomRepo, ReservationService reservationService,
                      ReservationRepository reservationRepo, MessageRepository messageRepo) {
        this.roomRepo = roomRepo;
        this.reservationService = reservationService;
        this.reservationRepo = reservationRepo;
        this.messageRepo = messageRepo;
    }

    public void getRooms(Context ctx) {
        try {
            List<Room> rooms = roomRepo.findAll();
            ctx.json(rooms.stream().map(this::roomToJson).toList());
        } catch (Exception e) {
            ctx.status(500).json(error("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void createReservation(Context ctx) {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail")
                    || !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut")
                    || !body.has("guests") || !body.has("ratePlan")) {
                ctx.status(400).json(error("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString());
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString());
            int guests = body.get("guests").getAsInt();
            RatePlan ratePlan = RatePlan.fromLabel(body.get("ratePlan").getAsString());

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(reservationToJson(reservation));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode()).json(error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Optional<Reservation> reservation = reservationRepo.findById(id);

            if (reservation.isEmpty()) {
                ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
            } else {
                ctx.json(reservationToJson(reservation.get()));
            }
        } catch (Exception e) {
            ctx.status(500).json(error("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (!body.has("amountPaid")) {
                ctx.status(400).json(error("INVALID_REQUEST", "Missing amountPaid"));
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
            Reservation confirmed = reservationService.confirmReservation(id, amountPaid);
            ctx.json(reservationToJson(confirmed));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode()).json(error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation cancelled = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(cancelled));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode()).json(error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation checkedIn = reservationService.checkIn(id);
            ctx.json(reservationToJson(checkedIn));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode()).json(error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation checkedOut = reservationService.checkOut(id);
            ctx.json(comprovanteToJson(reservationService.generateComprovante(checkedOut), checkedOut));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode()).json(error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(error("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Optional<Reservation> reservation = reservationRepo.findById(id);

            if (reservation.isEmpty()) {
                ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.get().getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
                ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Document only available for paid reservations"));
                return;
            }

            String comprovante = reservationService.generateComprovante(reservation.get());
            ctx.json(comprovanteToJson(comprovante, reservation.get()));
        } catch (Exception e) {
            ctx.status(500).json(error("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void getMessages(Context ctx) {
        try {
            List<Message> messages = messageRepo.findAll();
            ctx.json(messages.stream().map(this::messageToJson).toList());
        } catch (Exception e) {
            ctx.status(500).json(error("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(error("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            OccupancyReport report = new OccupancyReport(roomRepo, reservationRepo, date);
            ctx.contentType("text/csv; charset=utf-8").result(report.generate());
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(error("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr);
            RevenueReport report = new RevenueReport(reservationRepo, ym.getMonthValue(), ym.getYear());
            ctx.contentType("text/csv; charset=utf-8").result(report.generate());
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(error("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr);
            TourismTaxReport report = new TourismTaxReport(reservationRepo, ym.getMonthValue(), ym.getYear());
            ctx.contentType("text/csv; charset=utf-8").result(report.generate());
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRatePlanReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(error("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr);
            RatePlanReport report = new RatePlanReport(reservationRepo, ym.getMonthValue(), ym.getYear());
            ctx.contentType("text/csv; charset=utf-8").result(report.generate());
        } catch (Exception e) {
            ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
        }
    }

    private JsonObject roomToJson(Room room) {
        JsonObject obj = new JsonObject();
        obj.addProperty("number", room.getNumber());
        obj.addProperty("type", room.getType().getLabel());
        obj.addProperty("capacity", room.getCapacity());
        obj.addProperty("nightlyRate", Double.parseDouble(String.format("%.2f", room.getNightlyRate())));
        return obj;
    }

    private JsonObject reservationToJson(Reservation res) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", res.getId());
        obj.addProperty("roomNumber", res.getRoomNumber());
        obj.addProperty("guestName", res.getGuestName());
        obj.addProperty("guestEmail", res.getGuestEmail());
        obj.addProperty("guestCpf", res.getGuestCpf());
        obj.addProperty("checkIn", res.getCheckIn().toString());
        obj.addProperty("checkOut", res.getCheckOut().toString());
        obj.addProperty("guests", res.getGuests());
        obj.addProperty("ratePlan", res.getRatePlan().getLabel());
        obj.addProperty("nights", res.getNights());
        obj.addProperty("subtotal", Double.parseDouble(String.format("%.2f", res.getSubtotal())));
        obj.addProperty("discount", Double.parseDouble(String.format("%.2f", res.getDiscount())));
        obj.addProperty("total", Double.parseDouble(String.format("%.2f", res.getTotal())));
        obj.addProperty("status", res.getStatus().getLabel());
        obj.addProperty("amountPaid", Double.parseDouble(String.format("%.2f", res.getAmountPaid())));
        if (res.getRefund() != null) {
            obj.addProperty("refund", Double.parseDouble(String.format("%.2f", res.getRefund())));
        } else {
            obj.addProperty("refund", (Number) null);
        }
        return obj;
    }

    private JsonObject messageToJson(Message msg) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", msg.getId());
        obj.addProperty("channel", msg.getChannel());
        obj.addProperty("to", msg.getTo());
        obj.addProperty("subject", msg.getSubject());
        obj.addProperty("body", msg.getBody());
        return obj;
    }

    private JsonObject comprovanteToJson(String comprovante, Reservation res) {
        JsonObject obj = new JsonObject();
        obj.addProperty("title", "Comprovante de reserva");
        obj.addProperty("reservationId", res.getId());
        obj.addProperty("guestName", res.getGuestName());
        obj.addProperty("guestCpf", res.getGuestCpf());
        obj.addProperty("roomNumber", res.getRoomNumber());
        try {
            Optional<Room> room = roomRepo.findByNumber(res.getRoomNumber());
            obj.addProperty("roomType", room.isPresent() ? room.get().getType().getLabel() : "");
        } catch (Exception ignored) {
        }
        obj.addProperty("checkIn", res.getCheckIn().toString());
        obj.addProperty("checkOut", res.getCheckOut().toString());
        obj.addProperty("nights", res.getNights());
        obj.addProperty("subtotal", Double.parseDouble(String.format("%.2f", res.getSubtotal())));
        obj.addProperty("discount", Double.parseDouble(String.format("%.2f", res.getDiscount())));
        obj.addProperty("total", Double.parseDouble(String.format("%.2f", res.getTotal())));
        return obj;
    }

    private JsonObject error(String code, String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", code);
        obj.addProperty("message", message);
        return obj;
    }
}
