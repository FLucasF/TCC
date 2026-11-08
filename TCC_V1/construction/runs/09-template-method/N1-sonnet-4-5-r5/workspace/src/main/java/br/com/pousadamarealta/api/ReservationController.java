package br.com.pousadamarealta.api;

import br.com.pousadamarealta.domain.reservation.*;
import br.com.pousadamarealta.domain.room.Room;
import br.com.pousadamarealta.domain.room.RoomRepository;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService,
                                ReservationRepository reservationRepository,
                                RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void create(Context ctx) throws SQLException {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = (String) body.get("roomNumber");
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = ((Number) body.get("guests")).intValue();
            String ratePlanStr = (String) body.get("ratePlan");

            // Validações básicas
            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlanStr == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing required fields"
                ));
                return;
            }

            if (guests < 1) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "At least one guest is required"
                ));
                return;
            }

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Check-out must be after check-in"
                ));
                return;
            }

            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.create(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toResponse(reservation));

        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid date format"
            ));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("Room not found")) {
                ctx.status(404).json(Map.of(
                    "error", "ROOM_NOT_FOUND",
                    "message", "Room not found"
                ));
            } else if (e.getMessage().equals("Capacity exceeded")) {
                ctx.status(422).json(Map.of(
                    "error", "CAPACITY_EXCEEDED",
                    "message", "Number of guests exceeds room capacity"
                ));
            } else if (e.getMessage().equals("Room unavailable")) {
                ctx.status(409).json(Map.of(
                    "error", "ROOM_UNAVAILABLE",
                    "message", "Room is not available for the selected dates"
                ));
            } else {
                throw e;
            }
        }
    }

    public void get(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        }
    }

    public void confirm(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirm(id);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", e.getMessage()
            ));
        }
    }

    public void cancel(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancel(id);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", e.getMessage()
            ));
        }
    }

    public void checkIn(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", e.getMessage()
            ));
        }
    }

    public void checkOut(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            String document = reservationService.generateDocument(reservation);
            ctx.json(toDocumentResponse(reservation, document));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", e.getMessage()
            ));
        }
    }

    public void getDocument(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

            if (!reservation.isPaid()) {
                ctx.status(409).json(Map.of(
                    "error", "OPERATION_NOT_ALLOWED",
                    "message", "Document is only available for paid reservations"
                ));
                return;
            }

            String document = reservationService.generateDocument(reservation);
            ctx.json(toDocumentResponse(reservation, document));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        }
    }

    private Map<String, Object> toResponse(Reservation reservation) throws SQLException {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentResponse(Reservation reservation, String document) throws SQLException {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", room.getType().name());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }
}
