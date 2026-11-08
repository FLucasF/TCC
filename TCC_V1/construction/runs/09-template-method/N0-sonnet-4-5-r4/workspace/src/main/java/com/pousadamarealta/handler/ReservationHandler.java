package com.pousadamarealta.handler;

import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationHandler(ReservationService reservationService,
                            ReservationRepository reservationRepository,
                            RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.getRoomNumber() == null || request.getGuestName() == null ||
                request.getGuestEmail() == null || request.getGuestCpf() == null ||
                request.getCheckIn() == null || request.getCheckOut() == null ||
                request.getGuests() == null || request.getRatePlan() == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(request.getCheckIn());
                checkOut = LocalDate.parse(request.getCheckOut());
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.getRatePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

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

            ctx.status(201).json(toResponseMap(reservation));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException());

            ctx.json(toResponseMap(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toResponseMap(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toResponseMap(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toResponseMap(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            String receipt = reservationService.generateReceipt(reservation);

            var room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

            Map<String, Object> response = new HashMap<>();
            response.put("title", "Comprovante de reserva");
            response.put("reservationId", reservation.getId());
            response.put("guestName", reservation.getGuestName());
            response.put("guestCpf", reservation.getGuestCpf());
            response.put("roomNumber", reservation.getRoomNumber());
            response.put("roomType", room.getType().name());
            response.put("checkIn", reservation.getCheckIn().toString());
            response.put("checkOut", reservation.getCheckOut().toString());
            response.put("nights", reservation.getNights());
            response.put("subtotal", reservation.getSubtotal().doubleValue());
            response.put("discount", reservation.getDiscount().doubleValue());
            response.put("total", reservation.getTotal().doubleValue());

            ctx.json(response);

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException());

            var room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

            Map<String, Object> response = new HashMap<>();
            response.put("title", "Comprovante de reserva");
            response.put("reservationId", reservation.getId());
            response.put("guestName", reservation.getGuestName());
            response.put("guestCpf", reservation.getGuestCpf());
            response.put("roomNumber", reservation.getRoomNumber());
            response.put("roomType", room.getType().name());
            response.put("checkIn", reservation.getCheckIn().toString());
            response.put("checkOut", reservation.getCheckOut().toString());
            response.put("nights", reservation.getNights());
            response.put("subtotal", reservation.getSubtotal().doubleValue());
            response.put("discount", reservation.getDiscount().doubleValue());
            response.put("total", reservation.getTotal().doubleValue());

            ctx.json(response);

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot generate document for pending reservation"));
        }
    }

    private Map<String, Object> toResponseMap(Reservation reservation) {
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
        map.put("subtotal", reservation.getSubtotal().doubleValue());
        map.put("discount", reservation.getDiscount().doubleValue());
        map.put("total", reservation.getTotal().doubleValue());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid().doubleValue());
        map.put("refund", reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null);
        return map;
    }
}
