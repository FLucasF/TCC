package com.pousada.marealta.controller;

import com.google.gson.JsonSyntaxException;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationDocument;
import com.pousada.marealta.notification.NotificationService;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.util.DateFormatter;
import com.pousada.marealta.util.MoneyFormatter;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private static final ReservationRepository reservationRepository = new ReservationRepository();
    private static final RoomRepository roomRepository = new RoomRepository();
    private static final NotificationService notificationService = NotificationService.getInstance();

    private static final ReservationService reservationService =
        new ReservationService(reservationRepository, roomRepository, notificationService);

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                var req = ctx.bodyAsClass(CreateReservationRequest.class);
                validateCreateRequest(req);

                LocalDate checkIn = LocalDate.parse(req.checkIn);
                LocalDate checkOut = LocalDate.parse(req.checkOut);

                Reservation reservation = reservationService.create(
                    req.roomNumber,
                    req.guestName,
                    req.guestEmail,
                    req.guestCpf,
                    checkIn,
                    checkOut,
                    req.guests,
                    Reservation.RatePlan.valueOf(req.ratePlan)
                );

                ctx.status(201);
                ctx.json(toResponse(reservation));
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404);
                ctx.json(error("ROOM_NOT_FOUND", "Room not found"));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422);
                ctx.json(error("CAPACITY_EXCEEDED", "Too many guests for this room"));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409);
                ctx.json(error("ROOM_UNAVAILABLE", "Room is not available for the requested dates"));
            } catch (ReservationService.InvalidOperationException e) {
                ctx.status(400);
                ctx.json(error(e.getCode(), "Invalid request"));
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid date format"));
            } catch (JsonSyntaxException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid request body"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                ctx.json(toResponse(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirm(id);
                ctx.json(toResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.InvalidOperationException e) {
                ctx.status(409);
                ctx.json(error(e.getCode(), "Cannot confirm this reservation"));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancel(id);
                ctx.json(toResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.InvalidOperationException e) {
                ctx.status(409);
                ctx.json(error(e.getCode(), "Cannot cancel this reservation"));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(toResponse(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.InvalidOperationException e) {
                ctx.status(409);
                ctx.json(error(e.getCode(), "Cannot check in this reservation"));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                ReservationDocument doc = buildDocument(reservation);
                ctx.json(documentToResponse(doc));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404);
                ctx.json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.InvalidOperationException e) {
                ctx.status(409);
                ctx.json(error(e.getCode(), "Cannot check out this reservation"));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
                    ctx.status(409);
                    ctx.json(error("OPERATION_NOT_ALLOWED", "Document only available for confirmed reservations"));
                    return;
                }

                ReservationDocument doc = buildDocument(reservation);
                ctx.json(documentToResponse(doc));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });
    }

    private static void validateCreateRequest(CreateReservationRequest req) {
        if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null ||
            req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
            req.guests == 0 || req.ratePlan == null) {
            throw new ReservationService.InvalidOperationException("INVALID_REQUEST");
        }
    }

    private static Object toResponse(Reservation res) {
        return new Object() {
            public final long id = res.getId();
            public final int roomNumber = res.getRoomNumber();
            public final String guestName = res.getGuestName();
            public final String guestEmail = res.getGuestEmail();
            public final String guestCpf = res.getGuestCpf();
            public final String checkIn = DateFormatter.format(res.getCheckIn());
            public final String checkOut = DateFormatter.format(res.getCheckOut());
            public final int guests = res.getGuests();
            public final String ratePlan = res.getRatePlan().name();
            public final int nights = res.getNights();
            public final Object subtotal = res.getSubtotal();
            public final Object discount = res.getDiscount();
            public final Object total = res.getTotal();
            public final String status = res.getStatus().name();
            public final Object amountPaid = res.getAmountPaid();
            public final Object refund = res.getRefund();
        };
    }

    private static Object documentToResponse(ReservationDocument doc) {
        return new Object() {
            public final String title = doc.getTitle();
            public final long reservationId = doc.getReservationId();
            public final String guestName = doc.getGuestName();
            public final String guestCpf = doc.getGuestCpf();
            public final int roomNumber = doc.getRoomNumber();
            public final String roomType = doc.getRoomType();
            public final String checkIn = DateFormatter.format(doc.getCheckIn());
            public final String checkOut = DateFormatter.format(doc.getCheckOut());
            public final int nights = doc.getNights();
            public final Object subtotal = doc.getSubtotal();
            public final Object discount = doc.getDiscount();
            public final Object total = doc.getTotal();
        };
    }

    private static ReservationDocument buildDocument(Reservation res) throws SQLException {
        var room = roomRepository.findByNumber(res.getRoomNumber());
        String roomType = room != null ? room.getType().name() : "UNKNOWN";

        return new ReservationDocument(
            res.getId(),
            res.getGuestName(),
            res.getGuestCpf(),
            res.getRoomNumber(),
            roomType,
            res.getCheckIn(),
            res.getCheckOut(),
            res.getNights(),
            res.getSubtotal(),
            res.getDiscount(),
            res.getTotal()
        );
    }

    private static Object error(String code, String message) {
        String errorCode = code;
        String errorMessage = message;
        return new Object() {
            public final String error = errorCode;
            public final String message = errorMessage;
        };
    }

    public static class CreateReservationRequest {
        public int roomNumber;
        public String guestName;
        public String guestEmail;
        public String guestCpf;
        public String checkIn;
        public String checkOut;
        public int guests;
        public String ratePlan;
    }
}
