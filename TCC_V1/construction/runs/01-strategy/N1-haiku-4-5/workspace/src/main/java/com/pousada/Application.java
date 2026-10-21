package com.pousada;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.api.ApiException;
import com.pousada.api.dto.*;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.domain.rateplans.RatePlanFactory;
import com.pousada.service.DocumentService;
import com.pousada.service.MessageService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class Application {
    private static final ReservationService reservationService = new ReservationService();
    private static final MessageService messageService = new MessageService();
    private static final DocumentService documentService = new DocumentService(reservationService);
    private static final ReportService reportService = new ReportService(reservationService);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Javalin app = Javalin.create().start(port);

        // GET /rooms
        app.get("/rooms", ctx -> {
            List<RoomResponse> rooms = reservationService.getAllRooms().stream()
                .map(RoomResponse::fromRoom)
                .collect(Collectors.toList());
            ctx.json(rooms);
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = gson.fromJson(ctx.body(), CreateReservationRequest.class);
                validateCreateReservationRequest(req);

                LocalDate checkIn = LocalDate.parse(req.checkIn, DateTimeFormatter.ISO_DATE);
                LocalDate checkOut = LocalDate.parse(req.checkOut, DateTimeFormatter.ISO_DATE);

                Reservation reservation = reservationService.createReservation(
                    req.roomNumber,
                    req.guestName,
                    req.guestEmail,
                    req.guestCpf,
                    checkIn,
                    checkOut,
                    req.guests,
                    req.ratePlan,
                    req.companyCode
                );

                ctx.status(HttpStatus.CREATED);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                handleIllegalArgumentException(ctx, e.getMessage());
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
                }

                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
                }

                double amountPaid = reservation.getTotal();
                if (!ctx.body().isEmpty() && !ctx.body().equals("{}")) {
                    java.util.Map<String, Object> body = gson.fromJson(ctx.body(), java.util.Map.class);
                    if (body.containsKey("amountPaid")) {
                        amountPaid = ((Number) body.get("amountPaid")).doubleValue();
                    }
                }

                reservationService.confirmReservation(id, amountPaid);

                String emailBody = documentService.generateConfirmationEmail(reservation);
                messageService.sendEmail(reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);

                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (IllegalArgumentException e) {
                handleIllegalArgumentException(ctx, e.getMessage());
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
                }

                var ratePlan = RatePlanFactory.create(reservation.getRatePlan(), null);
                reservationService.cancelReservation(id, ratePlan);

                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (IllegalArgumentException e) {
                handleIllegalArgumentException(ctx, e.getMessage());
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
                }

                reservationService.checkIn(id);

                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (IllegalArgumentException e) {
                handleIllegalArgumentException(ctx, e.getMessage());
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
                }

                reservationService.checkOut(id);

                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (IllegalArgumentException e) {
                handleIllegalArgumentException(ctx, e.getMessage());
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
                }

                if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
                    reservation.getStatus() != ReservationStatus.CHECKED_IN &&
                    reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                    throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Document only available for confirmed reservations");
                }

                Room room = reservationService.getRoom(reservation.getRoomNumber());
                ctx.json(DocumentResponse.fromReservation(reservation, room));
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            List<MessageResponse> messages = messageService.getAllMessages().stream()
                .map(MessageResponse::fromMessage)
                .collect(Collectors.toList());
            ctx.json(messages);
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    throw new ApiException(400, "INVALID_REQUEST", "Missing date parameter");
                }

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                String report = reportService.generateOccupancyReport(date);

                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    throw new ApiException(400, "INVALID_REQUEST", "Missing month parameter");
                }

                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRevenueReport(month);

                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (ApiException e) {
                handleApiException(ctx, e);
            } catch (Exception e) {
                handleException(ctx, e);
            }
        });
    }

    private static void validateCreateReservationRequest(CreateReservationRequest req) {
        if (req.roomNumber == null || req.guestName == null || req.guestEmail == null ||
            req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
            req.guests == null || req.ratePlan == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing required fields");
        }
    }

    private static void handleIllegalArgumentException(io.javalin.http.Context ctx, String message) {
        if (message.equals("ROOM_NOT_FOUND")) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } else if (message.equals("ROOM_UNAVAILABLE")) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } else if (message.equals("CAPACITY_EXCEEDED")) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } else if (message.equals("OPERATION_NOT_ALLOWED")) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation state"));
        } else if (message.equals("RESERVATION_NOT_FOUND")) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } else {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", message));
        }
    }

    private static void handleApiException(io.javalin.http.Context ctx, ApiException e) {
        ctx.status(e.getStatusCode())
           .json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    private static void handleException(io.javalin.http.Context ctx, Exception e) {
        ctx.status(400)
           .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
    }
}
