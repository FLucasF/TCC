package com.pousada.api;

import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.DocumentResponse;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.domain.*;
import com.pousada.domain.rateplan.RatePlan;
import com.pousada.domain.rateplan.RatePlanFactory;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.DocumentService;
import com.pousada.service.PricingService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationController {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;

    public ReservationController(ReservationRepository reservationRepository,
                                 RoomRepository roomRepository,
                                 MessageRepository messageRepository,
                                 PricingService pricingService,
                                 DocumentService documentService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.pricingService = pricingService;
        this.documentService = documentService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequest(request);

            Room room = roomRepository.findByNumber(request.roomNumber())
                .orElseThrow(() -> new ValidationException("ROOM_NOT_FOUND", "Room not found", 404));

            if (request.guests() > room.getCapacity()) {
                throw new ValidationException("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity", 422);
            }

            if (reservationRepository.hasConflict(request.roomNumber(), request.checkIn(), request.checkOut(), null)) {
                throw new ValidationException("ROOM_UNAVAILABLE", "Room is not available for the selected dates", 409);
            }

            RatePlan ratePlan = RatePlanFactory.get(request.ratePlan());
            int nights = (int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());

            var subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), request.checkIn(), request.checkOut());
            var discount = pricingService.calculateDiscount(subtotal, ratePlan);
            var total = pricingService.calculateTotal(subtotal, discount);

            Reservation reservation = new Reservation(
                null,
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                ratePlan,
                nights,
                subtotal,
                discount,
                total
            );

            reservationRepository.save(reservation);

            ctx.status(201).json(toResponse(reservation));

        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

            ctx.json(toResponse(reservation));

        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

            reservation.confirm();
            reservationRepository.update(reservation);

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            Document document = documentService.createDocument(reservation, room);
            String documentText = documentService.formatDocument(document);
            String emailBody = documentService.createConfirmationEmail(reservation.getGuestName(), documentText);

            Message message = new Message(
                null,
                "EMAIL",
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                emailBody
            );
            messageRepository.save(message);

            ctx.json(toResponse(reservation));

        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

            reservation.cancel();
            reservationRepository.update(reservation);

            ctx.json(toResponse(reservation));

        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

            reservation.checkIn();
            reservationRepository.update(reservation);

            ctx.json(toResponse(reservation));

        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

            reservation.checkOut();
            reservationRepository.update(reservation);

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            Document document = documentService.createDocument(reservation, room);

            ctx.json(toDocumentResponse(document));

        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

            if (!reservation.isPaid()) {
                throw new ValidationException("OPERATION_NOT_ALLOWED", "Document is only available for paid reservations", 409);
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            Document document = documentService.createDocument(reservation, room);

            ctx.json(toDocumentResponse(document));

        } catch (ValidationException e) {
            ctx.status(e.statusCode).json(new ErrorResponse(e.errorCode, e.message));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null || request.ratePlan() == null) {
            throw new ValidationException("INVALID_REQUEST", "Missing required fields", 400);
        }

        if (request.guests() < 1) {
            throw new ValidationException("INVALID_REQUEST", "Number of guests must be at least 1", 400);
        }

        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new ValidationException("INVALID_REQUEST", "Check-out date must be after check-in date", 400);
        }
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan().getName(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
            document.getTitle(),
            document.getReservationId(),
            document.getGuestName(),
            document.getGuestCpf(),
            document.getRoomNumber(),
            document.getRoomType(),
            document.getCheckIn(),
            document.getCheckOut(),
            document.getNights(),
            document.getSubtotal(),
            document.getDiscount(),
            document.getTotal()
        );
    }

    private static class ValidationException extends RuntimeException {
        final String errorCode;
        final String message;
        final int statusCode;

        ValidationException(String errorCode, String message, int statusCode) {
            super(message);
            this.errorCode = errorCode;
            this.message = message;
            this.statusCode = statusCode;
        }
    }
}
