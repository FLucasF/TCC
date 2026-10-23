package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) throws SQLException {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            Reservation reservation = new Reservation();

            if (!json.has("roomNumber") || !json.has("guestName") || !json.has("guestEmail") ||
                !json.has("checkIn") || !json.has("checkOut") || !json.has("guests") || !json.has("ratePlan")) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            reservation.setRoomNumber(json.get("roomNumber").asText());
            reservation.setGuestName(json.get("guestName").asText());
            reservation.setGuestEmail(json.get("guestEmail").asText());
            reservation.setGuests(json.get("guests").asInt());

            try {
                reservation.setCheckIn(LocalDate.parse(json.get("checkIn").asText()));
                reservation.setCheckOut(LocalDate.parse(json.get("checkOut").asText()));
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
                reservation.getCheckOut().equals(reservation.getCheckIn())) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (reservation.getGuests() < 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "At least one guest required"));
                return;
            }

            try {
                reservation.setRatePlan(RatePlan.valueOf(json.get("ratePlan").asText()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            int identificationFields = 0;
            if (json.has("guestCpf") && !json.get("guestCpf").isNull()) {
                reservation.setGuestCpf(json.get("guestCpf").asText());
                identificationFields++;
            }
            if (json.has("companyCnpj") && !json.get("companyCnpj").isNull()) {
                if (!json.has("companyName") || json.get("companyName").isNull()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Company CNPJ requires company name"));
                    return;
                }
                reservation.setCompanyCnpj(json.get("companyCnpj").asText());
                reservation.setCompanyName(json.get("companyName").asText());
                identificationFields++;
            }
            if (json.has("guestPassport") && !json.get("guestPassport").isNull()) {
                if (!json.has("guestCountry") || json.get("guestCountry").isNull()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Guest passport requires guest country"));
                    return;
                }
                reservation.setGuestPassport(json.get("guestPassport").asText());
                reservation.setGuestCountry(json.get("guestCountry").asText());
                identificationFields++;
            }

            if (identificationFields != 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Exactly one guest identification method required"));
                return;
            }

            Optional<Room> room = roomService.getRoomByNumber(reservation.getRoomNumber());
            if (room.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            if (reservation.getGuests() > room.get().getCapacity()) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                return;
            }

            Reservation created = reservationService.createReservation(reservation);
            ctx.status(201).json(mapReservationToResponse(created, room.get()));

        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("Room unavailable")) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
            } else {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        }
    }

    public void getReservation(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Room room = roomService.getRoomByNumber(reservation.get().getRoomNumber())
                .orElseThrow(() -> new IllegalStateException("Room not found"));

            ctx.json(mapReservationToResponse(reservation.get(), room));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void confirmReservation(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalStateException("Room not found"));

            ctx.json(mapReservationToResponse(reservation, room));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm reservation in current status"));
        }
    }

    public void checkIn(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalStateException("Room not found"));

            ctx.json(mapReservationToResponse(reservation, room));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check-in reservation in current status"));
        }
    }

    public void checkOut(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.checkOut(id);

            ctx.json(mapDocumentToResponse(document));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check-out reservation in current status"));
        }
    }

    public void cancelReservation(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalStateException("Room not found"));

            ctx.json(mapReservationToResponse(reservation, room));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current status"));
        }
    }

    public void getDocument(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.getDocument(id);

            ctx.json(mapDocumentToResponse(document));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not available"));
        }
    }

    private Map<String, Object> mapReservationToResponse(Reservation reservation, Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("companyCnpj", reservation.getCompanyCnpj());
        map.put("companyName", reservation.getCompanyName());
        map.put("guestPassport", reservation.getGuestPassport());
        map.put("guestCountry", reservation.getGuestCountry());
        map.put("checkIn", reservation.getCheckIn());
        map.put("checkOut", reservation.getCheckOut());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal(room));
        map.put("discount", reservation.getDiscount(reservation.getSubtotal(room)));
        map.put("total", reservation.getTotal(room));
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> mapDocumentToResponse(Document document) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", document.getTitle());
        map.put("reservationId", document.getReservationId());
        map.put("guestName", document.getGuestName());
        map.put("guestCpf", document.getGuestCpf());
        map.put("companyCnpj", document.getCompanyCnpj());
        map.put("companyName", document.getCompanyName());
        map.put("guestPassport", document.getGuestPassport());
        map.put("guestCountry", document.getGuestCountry());
        map.put("roomNumber", document.getRoomNumber());
        map.put("roomType", document.getRoomType().name());
        map.put("checkIn", document.getCheckIn());
        map.put("checkOut", document.getCheckOut());
        map.put("nights", document.getNights());
        map.put("subtotal", document.getSubtotal());
        map.put("discount", document.getDiscount());
        map.put("total", document.getTotal());
        map.put("text", document.getText());
        return map;
    }
}
