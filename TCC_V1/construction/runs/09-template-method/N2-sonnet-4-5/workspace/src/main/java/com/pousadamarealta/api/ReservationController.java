package com.pousadamarealta.api;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final DocumentService documentService;

    public ReservationController(
            ReservationService reservationService,
            RoomRepository roomRepository,
            DocumentService documentService) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.documentService = documentService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            Reservation reservation = new Reservation();
            reservation.setRoomNumber(getString(body, "roomNumber"));
            reservation.setGuestName(getString(body, "guestName"));
            reservation.setGuestEmail(getString(body, "guestEmail"));
            reservation.setGuestCpf(getString(body, "guestCpf"));
            reservation.setCheckIn(getDate(body, "checkIn"));
            reservation.setCheckOut(getDate(body, "checkOut"));
            reservation.setGuests(getInt(body, "guests"));
            reservation.setRatePlan(RatePlan.valueOf(getString(body, "ratePlan")));

            Reservation created = reservationService.createReservation(reservation);
            ctx.status(201).json(toDto(created));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request format"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findById(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            Document document = documentService.createDocument(reservation, room);
            ctx.json(toDocumentDto(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findById(id);

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            Document document = documentService.createDocument(reservation, room);
            ctx.json(toDocumentDto(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private Map<String, Object> toDto(Reservation r) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", r.getId());
        dto.put("roomNumber", r.getRoomNumber());
        dto.put("guestName", r.getGuestName());
        dto.put("guestEmail", r.getGuestEmail());
        dto.put("guestCpf", r.getGuestCpf());
        dto.put("checkIn", r.getCheckIn().toString());
        dto.put("checkOut", r.getCheckOut().toString());
        dto.put("guests", r.getGuests());
        dto.put("ratePlan", r.getRatePlan().name());
        dto.put("nights", r.getNights());
        dto.put("subtotal", r.getSubtotal());
        dto.put("discount", r.getDiscount());
        dto.put("total", r.getTotal());
        dto.put("status", r.getStatus().name());
        dto.put("amountPaid", r.getAmountPaid());
        dto.put("refund", r.getRefund());
        return dto;
    }

    private Map<String, Object> toDocumentDto(Document doc) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", doc.getTitle());
        dto.put("reservationId", doc.getReservationId());
        dto.put("guestName", doc.getGuestName());
        dto.put("guestCpf", doc.getGuestCpf());
        dto.put("roomNumber", doc.getRoomNumber());
        dto.put("roomType", doc.getRoomType().name());
        dto.put("checkIn", doc.getCheckIn().toString());
        dto.put("checkOut", doc.getCheckOut().toString());
        dto.put("nights", doc.getNights());
        dto.put("subtotal", doc.getSubtotal());
        dto.put("discount", doc.getDiscount());
        dto.put("total", doc.getTotal());
        return dto;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return value.toString();
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private LocalDate getDate(Map<String, Object> map, String key) {
        try {
            return LocalDate.parse(getString(map, key));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for " + key);
        }
    }
}
