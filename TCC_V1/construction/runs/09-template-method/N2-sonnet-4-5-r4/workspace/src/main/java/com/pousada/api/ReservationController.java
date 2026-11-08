package com.pousada.api;

import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.DocumentResponse;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.service.DocumentGenerator;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final DocumentGenerator documentGenerator;

    public ReservationController(ReservationService reservationService, DocumentGenerator documentGenerator) {
        this.reservationService = reservationService;
        this.documentGenerator = documentGenerator;
    }

    public void register(Javalin app) {
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.getRoomNumber() == null || request.getGuestName() == null ||
                request.getGuestEmail() == null || request.getGuestCpf() == null ||
                request.getCheckIn() == null || request.getCheckOut() == null ||
                request.getGuests() == null || request.getRatePlan() == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            LocalDate checkIn = LocalDate.parse(request.getCheckIn());
            LocalDate checkOut = LocalDate.parse(request.getCheckOut());
            RatePlan ratePlan = RatePlan.valueOf(request.getRatePlan());

            Reservation reservation = reservationService.createReservation(
                request.getRoomNumber(),
                request.getGuestName(),
                request.getGuestEmail(),
                request.getGuestCpf(),
                checkIn,
                checkOut,
                request.getGuests(),
                ratePlan
            );

            ctx.status(HttpStatus.CREATED);
            ctx.json(new ReservationResponse(reservation));

        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format or rate plan"));
        } catch (RoomService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.status(HttpStatus.OK);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.status(HttpStatus.OK);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.status(HttpStatus.OK);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    private void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.status(HttpStatus.OK);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    private void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            var document = documentGenerator.generateDocument(reservation);
            ctx.status(HttpStatus.OK);
            ctx.json(new DocumentResponse(document));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    private void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.isPaid()) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            var document = documentGenerator.generateDocument(reservation);
            ctx.status(HttpStatus.OK);
            ctx.json(new DocumentResponse(document));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }
}
