package com.marealata.handler;

import com.marealata.dao.*;
import com.marealata.model.*;
import com.marealata.service.*;
import com.marealata.util.DocumentFormatter;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationHandler {
    static class CreateReservationRequest {
        public int roomNumber;
        public String guestName;
        public String guestEmail;
        public String guestCpf;
        public LocalDate checkIn;
        public LocalDate checkOut;
        public int guests;
        public RatePlan ratePlan;
    }

    static class ConfirmReservationRequest {
        public BigDecimal amountPaid;
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

                if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null ||
                    req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
                    req.guests == 0 || req.ratePlan == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                Reservation reservation = ReservationService.createReservation(
                    req.roomNumber, req.guestName, req.guestEmail, req.guestCpf,
                    req.checkIn, req.checkOut, req.guests, req.ratePlan
                );

                ctx.status(201).json(reservation);
            } catch (ReservationException e) {
                int status = switch (e.errorCode) {
                    case "ROOM_NOT_FOUND" -> 404;
                    case "CAPACITY_EXCEEDED" -> 422;
                    case "ROOM_UNAVAILABLE" -> 409;
                    default -> 400;
                };
                ctx.status(status).json(new ErrorResponse(e.errorCode, e.message));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationDao.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.json(reservation);
                }
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                ConfirmReservationRequest req = ctx.bodyAsClass(ConfirmReservationRequest.class);

                if (req.amountPaid == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "amountPaid is required"));
                    return;
                }

                Reservation reservation = ReservationService.confirmReservation(id, req.amountPaid);
                ctx.json(reservation);
            } catch (ReservationException e) {
                int status = switch (e.errorCode) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 400;
                };
                ctx.status(status).json(new ErrorResponse(e.errorCode, e.message));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.cancelReservation(id);
                ctx.json(reservation);
            } catch (ReservationException e) {
                int status = switch (e.errorCode) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 400;
                };
                ctx.status(status).json(new ErrorResponse(e.errorCode, e.message));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkIn(id);
                ctx.json(reservation);
            } catch (ReservationException e) {
                int status = switch (e.errorCode) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 400;
                };
                ctx.status(status).json(new ErrorResponse(e.errorCode, e.message));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkOut(id);

                Room room = RoomDao.findByNumber(reservation.roomNumber);
                String roomType = room != null ? room.type : "UNKNOWN";

                ReservationDocument doc = new ReservationDocument(
                    reservation.id, reservation.guestName, reservation.guestCpf,
                    reservation.roomNumber, roomType, reservation.checkIn, reservation.checkOut,
                    reservation.nights, reservation.subtotal, reservation.discount, reservation.total
                );

                ctx.json(doc);
            } catch (ReservationException e) {
                int status = switch (e.errorCode) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 400;
                };
                ctx.status(status).json(new ErrorResponse(e.errorCode, e.message));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationDao.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.status == ReservationStatus.PENDING) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED",
                        "Document only available for paid reservations"));
                    return;
                }

                Room room = RoomDao.findByNumber(reservation.roomNumber);
                String roomType = room != null ? room.type : "UNKNOWN";

                ReservationDocument doc = new ReservationDocument(
                    reservation.id, reservation.guestName, reservation.guestCpf,
                    reservation.roomNumber, roomType, reservation.checkIn, reservation.checkOut,
                    reservation.nights, reservation.subtotal, reservation.discount, reservation.total
                );

                ctx.json(doc);
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }
}
