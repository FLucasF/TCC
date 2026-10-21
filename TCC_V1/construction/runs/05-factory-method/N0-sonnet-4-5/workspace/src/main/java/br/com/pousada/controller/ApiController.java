package br.com.pousada.controller;

import br.com.pousada.model.*;
import br.com.pousada.service.*;
import br.com.pousada.util.DateFormatter;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ApiController {

    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;

    public ApiController(RoomService roomService, ReservationService reservationService,
                        MessageService messageService, ReportService reportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(Map.of(
                "error", "ROOM_NOT_FOUND",
                "message", "Room not found"
            ));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of(
                "error", "ROOM_UNAVAILABLE",
                "message", "Room is not available for the selected dates"
            ));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Operation not allowed in current reservation status"
            ));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(Map.of(
                "error", "CAPACITY_EXCEEDED",
                "message", "Number of guests exceeds room capacity"
            ));
        });

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", e.getMessage()
            ));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", e.getMessage()
            ));
        });
    }

    private void getRooms(Context ctx) {
        ctx.json(roomService.getAllRooms());
    }

    private void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = (String) body.get("roomNumber");
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = body.get("guests") instanceof Integer ? (Integer) body.get("guests") : null;
            String ratePlanStr = (String) body.get("ratePlan");

            String guestCpf = (String) body.get("guestCpf");
            String companyCnpj = (String) body.get("companyCnpj");
            String companyName = (String) body.get("companyName");
            String guestPassport = (String) body.get("guestPassport");
            String guestCountry = (String) body.get("guestCountry");

            if (roomNumber == null || guestName == null || guestEmail == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlanStr == null) {
                throw new IllegalArgumentException("Missing required fields");
            }

            int identificationCount = 0;
            if (guestCpf != null) identificationCount++;
            if (companyCnpj != null && companyName != null) identificationCount++;
            if (guestPassport != null && guestCountry != null) identificationCount++;

            if (identificationCount != 1) {
                throw new IllegalArgumentException("Invalid identification: must provide exactly one of: guestCpf, companyCnpj+companyName, or guestPassport+guestCountry");
            }

            if (companyCnpj != null && companyName == null) {
                throw new IllegalArgumentException("companyCnpj requires companyName");
            }
            if (companyName != null && companyCnpj == null) {
                throw new IllegalArgumentException("companyName requires companyCnpj");
            }
            if (guestPassport != null && guestCountry == null) {
                throw new IllegalArgumentException("guestPassport requires guestCountry");
            }
            if (guestCountry != null && guestPassport == null) {
                throw new IllegalArgumentException("guestCountry requires guestPassport");
            }

            var checkIn = DateFormatter.parseIso(checkInStr);
            var checkOut = DateFormatter.parseIso(checkOutStr);
            var ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail,
                guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(reservation);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format");
        }
    }

    private void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id)
            .orElseThrow(() -> new ReservationService.ReservationNotFoundException());
        ctx.json(reservation);
    }

    private void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(reservation);
    }

    private void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(reservation);
    }

    private void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(reservation);
    }

    private void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(document);
    }

    private void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(document);
    }

    private void getMessages(Context ctx) {
        ctx.json(messageService.getAllMessages());
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                throw new IllegalArgumentException("Missing date parameter");
            }
            var date = DateFormatter.parseIso(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format");
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                throw new IllegalArgumentException("Missing month parameter");
            }
            var month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format");
        }
    }
}
