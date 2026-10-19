package com.pousada.api;

import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.api.dto.RoomResponse;
import com.pousada.api.dto.MessageResponse;
import com.pousada.models.Message;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.services.HotelService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ApiRoutes {
    public static void setup(Javalin app, HotelService hotelService) {
        app.get("/rooms", ctx -> {
            Map<Integer, Room> rooms = hotelService.getAllRooms();
            List<RoomResponse> response = rooms.values().stream()
                    .map(RoomResponse::fromRoom)
                    .toList();
            ctx.json(response);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

                if (req.roomNumber() == null || req.guestName() == null || req.guestEmail() == null
                        || req.guestCpf() == null || req.checkIn() == null || req.checkOut() == null
                        || req.guests() == null || req.ratePlan() == null) {
                    ctx.status(400);
                    ctx.result(ErrorResponse.error("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                Reservation reservation = hotelService.createReservation(
                        req.roomNumber(),
                        req.guestName(),
                        req.guestEmail(),
                        req.guestCpf(),
                        req.checkIn(),
                        req.checkOut(),
                        req.guests(),
                        req.ratePlan());

                ctx.status(201);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                String error = e.getMessage();
                if ("ROOM_NOT_FOUND".equals(error)) {
                    ctx.status(404);
                    ctx.result(ErrorResponse.error("ROOM_NOT_FOUND", "Room not found"));
                } else if ("ROOM_UNAVAILABLE".equals(error)) {
                    ctx.status(409);
                    ctx.result(ErrorResponse.error("ROOM_UNAVAILABLE", "Room is not available"));
                } else if ("CAPACITY_EXCEEDED".equals(error)) {
                    ctx.status(422);
                    ctx.result(ErrorResponse.error("CAPACITY_EXCEEDED", "Guest count exceeds room capacity"));
                } else {
                    ctx.status(400);
                    ctx.result(ErrorResponse.error("INVALID_REQUEST", error));
                }
            } catch (Exception e) {
                ctx.status(400);
                ctx.result(ErrorResponse.error("INVALID_REQUEST", "Invalid request body"));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = hotelService.getReservation(id);
                if (reservation == null) {
                    throw new NotFoundResponse(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                }
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (NotFoundResponse e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = hotelService.confirmReservation(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                String error = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(error)) {
                    throw new NotFoundResponse(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else if ("OPERATION_NOT_ALLOWED".equals(error)) {
                    throw new ConflictResponse(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation"));
                } else {
                    throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", error));
                }
            } catch (Exception e) {
                throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = hotelService.cancelReservation(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                String error = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(error)) {
                    throw new NotFoundResponse(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else if ("OPERATION_NOT_ALLOWED".equals(error)) {
                    throw new ConflictResponse(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation"));
                } else {
                    throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", error));
                }
            } catch (Exception e) {
                throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = hotelService.checkInReservation(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                String error = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(error)) {
                    throw new NotFoundResponse(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else if ("OPERATION_NOT_ALLOWED".equals(error)) {
                    throw new ConflictResponse(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation"));
                } else {
                    throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", error));
                }
            } catch (Exception e) {
                throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                hotelService.checkOutReservation(id);
                String receipt = hotelService.getReceipt(id);
                ctx.result(receipt);
            } catch (IllegalArgumentException e) {
                String error = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(error)) {
                    throw new NotFoundResponse(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else if ("OPERATION_NOT_ALLOWED".equals(error)) {
                    throw new ConflictResponse(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation"));
                } else {
                    throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", error));
                }
            } catch (Exception e) {
                throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                String receipt = hotelService.getReceipt(id);
                ctx.result(receipt);
            } catch (IllegalArgumentException e) {
                throw new NotFoundResponse(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (Exception e) {
                throw new BadRequestResponse(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/messages", ctx -> {
            List<Message> messages = hotelService.getMessages();
            List<MessageResponse> response = messages.stream()
                    .map(MessageResponse::fromMessage)
                    .toList();
            ctx.json(response);
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.result(ErrorResponse.error("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String report = hotelService.getOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.result(ErrorResponse.error("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.result(ErrorResponse.error("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                ctx.status(400);
                ctx.result(ErrorResponse.error("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                if (month < 1 || month > 12) {
                    ctx.status(400);
                    ctx.result(ErrorResponse.error("INVALID_REQUEST", "Invalid month value"));
                    return;
                }

                String report = hotelService.getRevenueReport(year, month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.result(ErrorResponse.error("INVALID_REQUEST", "Invalid month format"));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500);
                ctx.result(ErrorResponse.error("INTERNAL_ERROR", "Internal server error"));
            }
        });
    }
}
