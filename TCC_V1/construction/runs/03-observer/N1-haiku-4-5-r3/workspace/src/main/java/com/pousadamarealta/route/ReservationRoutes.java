package com.pousadamarealta.route;

import com.pousadamarealta.dto.*;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;

public class ReservationRoutes {
    public static void register(Javalin app, ReservationService reservationService) {
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
                Reservation reservation = reservationService.createReservation(request);
                ctx.status(201);
                ctx.json(toResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(toResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(toResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(toResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(toResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                DocumentResponse document = toDocumentResponse(reservation, reservationService);
                ctx.json(document);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation.getAmountPaid().signum() == 0) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not available for unpaid reservations"));
                    return;
                }

                DocumentResponse document = toDocumentResponse(reservation, reservationService);
                ctx.json(document);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }

    private static ReservationResponse toResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn().toString();
        response.checkOut = reservation.getCheckOut().toString();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().toString();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus().toString();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();
        return response;
    }

    private static DocumentResponse toDocumentResponse(Reservation reservation, ReservationService reservationService) {
        DocumentResponse response = new DocumentResponse();
        response.title = "Comprovante de reserva";
        response.reservationId = reservation.getId();
        response.guestName = reservation.getGuestName();
        response.guestCpf = formatCpf(reservation.getGuestCpf());
        response.roomNumber = reservation.getRoomNumber();
        response.roomType = getRoomType(reservation.getRoomNumber());
        response.checkIn = formatDate(reservation.getCheckIn());
        response.checkOut = formatDate(reservation.getCheckOut());
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        return response;
    }

    private static void handleError(io.javalin.http.Context ctx, String error) {
        if (error.equals("ROOM_NOT_FOUND")) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } else if (error.equals("RESERVATION_NOT_FOUND")) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } else if (error.equals("ROOM_UNAVAILABLE")) {
            ctx.status(409);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
        } else if (error.equals("OPERATION_NOT_ALLOWED")) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "This operation is not allowed in the current reservation state"));
        } else if (error.equals("CAPACITY_EXCEEDED")) {
            ctx.status(422);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } else if (error.equals("INVALID_REQUEST")) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        } else {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", error));
        }
    }

    private static String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9));
    }

    private static String getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
