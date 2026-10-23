package com.pousada.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.pousada.domain.Message;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.service.MessageService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ApiController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiController(RoomService roomService, ReservationService reservationService,
                        MessageService messageService, ReportService reportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomService.getAllRooms();
        ArrayNode json = mapper.createArrayNode();
        for (Room room : rooms) {
            json.add(ApiMappers.toRoomJson(room));
        }
        ctx.json(json);
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode body = mapper.readTree(ctx.body());

            String roomNumber = getRequiredString(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = getRequiredDate(body, "checkIn");
            LocalDate checkOut = getRequiredDate(body, "checkOut");
            int guests = getRequiredInt(body, "guests");
            String ratePlan = getRequiredString(body, "ratePlan");

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(ApiMappers.toReservationJson(reservation));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON"));
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException());
            ctx.json(ApiMappers.toReservationJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ApiMappers.toReservationJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ApiMappers.toReservationJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ApiMappers.toReservationJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
            String document = messageService.formatDocument(reservation, room.getType().name());

            ctx.json(ApiMappers.toDocumentJson(reservation, room.getType().name(), document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException());

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
            String document = messageService.formatDocument(reservation, room.getType().name());

            ctx.json(ApiMappers.toDocumentJson(reservation, room.getType().name(), document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        ArrayNode json = mapper.createArrayNode();
        for (Message message : messages) {
            json.add(ApiMappers.toMessageJson(message));
        }
        ctx.json(json);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private String getRequiredString(JsonNode body, String field) {
        if (!body.has(field) || body.get(field).isNull()) {
            throw new InvalidRequestException("Missing required field: " + field);
        }
        return body.get(field).asText();
    }

    private int getRequiredInt(JsonNode body, String field) {
        if (!body.has(field) || body.get(field).isNull()) {
            throw new InvalidRequestException("Missing required field: " + field);
        }
        return body.get(field).asInt();
    }

    private LocalDate getRequiredDate(JsonNode body, String field) {
        String dateStr = getRequiredString(body, field);
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format for field: " + field);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
