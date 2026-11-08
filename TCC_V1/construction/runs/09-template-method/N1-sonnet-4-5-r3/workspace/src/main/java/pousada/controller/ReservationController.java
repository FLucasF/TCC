package pousada.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.dto.CreateReservationRequest;
import pousada.dto.ErrorResponse;
import pousada.repository.RoomRepository;
import pousada.service.DocumentFormatter;
import pousada.service.ReservationService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequest(request);

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

            ctx.status(HttpStatus.CREATED).json(toResponse(reservation));

        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException("Reservation not found"));

            ctx.json(toResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

            ctx.json(toDocumentResponse(reservation, room));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException("Reservation not found"));

            if (!reservation.isPaid()) {
                throw new ReservationService.OperationNotAllowedException("Document only available for paid reservations");
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

            ctx.json(toDocumentResponse(reservation, room));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getGuestCpf() == null ||
            request.getCheckIn() == null || request.getCheckOut() == null ||
            request.getGuests() == null || request.getRatePlan() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
    }

    private Map<String, Object> toResponse(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentResponse(Reservation reservation, Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", room.getType().name());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }
}
