package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.dto.*;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    private static final RoomService roomService = new RoomService();
    private static final MessageService messageService = new MessageService();
    private static final ReservationService reservationService = new ReservationService(roomService, messageService);
    private static final ReportService reportService = new ReportService(reservationService, roomService);

    public static void main(String[] args) {
        int port = getPort();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new io.javalin.json.JavalinJackson(objectMapper, true));
        }).start(port);

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(io.javalin.http.BadRequestResponse.class, (e, ctx) -> {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de dados inválido"));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });

        app.get("/rooms", Main::getRooms);
        app.post("/reservations", Main::createReservation);
        app.get("/reservations/{id}", Main::getReservation);
        app.post("/reservations/{id}/confirmation", Main::confirmReservation);
        app.post("/reservations/{id}/cancellation", Main::cancelReservation);
        app.post("/reservations/{id}/check-in", Main::checkIn);
        app.post("/reservations/{id}/check-out", Main::checkOut);
        app.get("/reservations/{id}/document", Main::getDocument);
        app.get("/messages", Main::getMessages);
        app.get("/reports/occupancy", Main::getOccupancyReport);
        app.get("/reports/revenue", Main::getRevenueReport);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }

    private static void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::new)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }

    private static void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getGuestCpf() == null ||
            request.getCheckIn() == null || request.getCheckOut() == null ||
            request.getRatePlan() == null) {
            throw new InvalidRequestException("Campos obrigatórios faltando");
        }

        Reservation reservation = reservationService.createReservation(
            request.getRoomNumber(),
            request.getGuestName(),
            request.getGuestEmail(),
            request.getGuestCpf(),
            request.getCheckIn(),
            request.getCheckOut(),
            request.getGuests(),
            request.getRatePlan(),
            request.getCompanyCode()
        );

        ctx.status(HttpStatus.CREATED);
        ctx.json(new ReservationResponse(reservation));
    }

    private static void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private static void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private static void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private static void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(new ReservationResponse(reservation));
    }

    private static void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomService.getRoom(reservation.getRoomNumber());

        DocumentResponse document = new DocumentResponse(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        ctx.json(document);
    }

    private static void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new InvalidRequestException("Documento disponível apenas para reservas pagas");
        }

        Room room = roomService.getRoom(reservation.getRoomNumber());

        DocumentResponse document = new DocumentResponse(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        ctx.json(document);
    }

    private static void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::new)
            .collect(Collectors.toList());
        ctx.json(messages);
    }

    private static void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new InvalidRequestException("Parâmetro 'date' é obrigatório");
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de data inválido. Use yyyy-MM-dd");
        }
    }

    private static void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Parâmetro 'month' é obrigatório");
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de mês inválido. Use yyyy-MM");
        }
    }
}
