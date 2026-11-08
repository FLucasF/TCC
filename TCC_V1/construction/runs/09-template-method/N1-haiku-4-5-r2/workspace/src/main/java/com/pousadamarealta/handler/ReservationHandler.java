package com.pousadamarealta.handler;

import io.javalin.http.Context;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.ValidationException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.Formatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReservationHandler {
    private ReservationService reservationService;
    private RoomRepository roomRepository;

    public ReservationHandler(Database database) {
        this.reservationService = new ReservationService(database);
        this.roomRepository = new RoomRepository(database);
    }

    public void createReservation(Context ctx) throws Exception {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            int roomNumber = getInt(body, "roomNumber");
            String guestName = getString(body, "guestName");
            String guestEmail = getString(body, "guestEmail");
            String guestCpf = getString(body, "guestCpf");
            LocalDate checkIn = parseDate(getString(body, "checkIn"));
            LocalDate checkOut = parseDate(getString(body, "checkOut"));
            int guests = getInt(body, "guests");
            String ratePlanStr = getString(body, "ratePlan");

            if (roomNumber == 0 || guestName == null || guestEmail == null || guestCpf == null ||
                checkIn == null || checkOut == null || guests == 0 || ratePlanStr == null) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
                return;
            }

            if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date range"));
                return;
            }

            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);
            Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail,
                                                                           guestCpf, checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toDto(reservation));
        } catch (ValidationException e) {
            int status = switch (e.getErrorCode()) {
                case "ROOM_NOT_FOUND" -> 404;
                case "CAPACITY_EXCEEDED" -> 422;
                case "ROOM_UNAVAILABLE" -> 409;
                default -> 400;
            };
            ctx.status(status).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    public void getReservation(Context ctx) throws Exception {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toDto(reservation));
        } catch (ValidationException e) {
            ctx.status(404).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    public void confirmReservation(Context ctx) throws Exception {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(toDto(reservation));
        } catch (ValidationException e) {
            ctx.status(409).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    public void cancelReservation(Context ctx) throws Exception {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toDto(reservation));
        } catch (ValidationException e) {
            ctx.status(409).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    public void checkIn(Context ctx) throws Exception {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toDto(reservation));
        } catch (ValidationException e) {
            ctx.status(409).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    public void checkOut(Context ctx) throws Exception {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            String comprovante = reservationService.buildComprovante(reservation, room);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("title", "Comprovante de reserva");
            response.put("reservationId", reservation.getId());
            response.put("guestName", reservation.getGuestName());
            response.put("guestCpf", reservation.getGuestCpf());
            response.put("roomNumber", reservation.getRoomNumber());
            response.put("roomType", room.getType().name());
            response.put("checkIn", Formatter.formatIsoDate(reservation.getCheckIn()));
            response.put("checkOut", Formatter.formatIsoDate(reservation.getCheckOut()));
            response.put("nights", reservation.getNights());
            response.put("subtotal", reservation.getSubtotal());
            response.put("discount", reservation.getDiscount());
            response.put("total", reservation.getTotal());

            ctx.json(response);
        } catch (ValidationException e) {
            ctx.status(409).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    public void getDocument(Context ctx) throws Exception {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.getStatus().name().matches("CONFIRMED|CHECKED_IN|CHECKED_OUT")) {
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Document not available"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            String comprovante = reservationService.buildComprovante(reservation, room);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("title", "Comprovante de reserva");
            response.put("reservationId", reservation.getId());
            response.put("guestName", reservation.getGuestName());
            response.put("guestCpf", reservation.getGuestCpf());
            response.put("roomNumber", reservation.getRoomNumber());
            response.put("roomType", room.getType().name());
            response.put("checkIn", Formatter.formatIsoDate(reservation.getCheckIn()));
            response.put("checkOut", Formatter.formatIsoDate(reservation.getCheckOut()));
            response.put("nights", reservation.getNights());
            response.put("subtotal", reservation.getSubtotal());
            response.put("discount", reservation.getDiscount());
            response.put("total", reservation.getTotal());

            ctx.json(response);
        } catch (ValidationException e) {
            ctx.status(404).json(Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    private ReservationDto toDto(Reservation reservation) {
        return new ReservationDto(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            Formatter.formatIsoDate(reservation.getCheckIn()),
            Formatter.formatIsoDate(reservation.getCheckOut()),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }

    private void handleError(Context ctx, Exception e) {
        ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static class ReservationDto {
        public Long id;
        public int roomNumber;
        public String guestName;
        public String guestEmail;
        public String guestCpf;
        public String checkIn;
        public String checkOut;
        public int guests;
        public String ratePlan;
        public int nights;
        public Object subtotal;
        public Object discount;
        public Object total;
        public String status;
        public Object amountPaid;
        public Object refund;

        public ReservationDto(Long id, int roomNumber, String guestName, String guestEmail,
                             String guestCpf, String checkIn, String checkOut, int guests,
                             String ratePlan, int nights, Object subtotal, Object discount,
                             Object total, String status, Object amountPaid, Object refund) {
            this.id = id;
            this.roomNumber = roomNumber;
            this.guestName = guestName;
            this.guestEmail = guestEmail;
            this.guestCpf = guestCpf;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.guests = guests;
            this.ratePlan = ratePlan;
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.status = status;
            this.amountPaid = amountPaid;
            this.refund = refund;
        }
    }
}
