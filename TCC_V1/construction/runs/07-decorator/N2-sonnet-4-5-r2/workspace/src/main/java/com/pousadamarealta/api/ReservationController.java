package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentResponse;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.api.dto.ReservationResponse;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final DocumentService documentService;

    public ReservationController(ReservationService reservationService,
                                RoomRepository roomRepository,
                                DocumentService documentService) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.documentService = documentService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            LocalDate checkIn = LocalDate.parse(request.checkIn());
            LocalDate checkOut = LocalDate.parse(request.checkOut());
            RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                checkIn,
                checkOut,
                request.guests(),
                ratePlan
            );

            ctx.status(HttpStatus.CREATED);
            ctx.json(ReservationResponse.from(reservation));

        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request data"));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Guest count exceeds room capacity"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
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

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
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

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
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

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            DocumentResponse document = createDocumentResponse(reservation);
            ctx.json(document);
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

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().signum() == 0) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document is only available for paid reservations"));
                return;
            }

            DocumentResponse document = createDocumentResponse(reservation);
            ctx.json(document);
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private DocumentResponse createDocumentResponse(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        return new DocumentResponse(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
