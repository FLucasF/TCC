package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
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
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequest(req);

            LocalDate checkIn = parseDate(req.checkIn());
            LocalDate checkOut = parseDate(req.checkOut());
            RatePlan ratePlan = RatePlan.valueOf(req.ratePlan());

            Reservation reservation = reservationService.createReservation(
                req.roomNumber(),
                req.guestName(),
                req.guestEmail(),
                req.guestCpf(),
                checkIn,
                checkOut,
                req.guests(),
                ratePlan
            );

            ctx.status(201).json(toDto(reservation));

        } catch (IllegalArgumentException e) {
            ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Room not found"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(ApiResponse.error(e.getErrorCode(), "Room unavailable for selected dates"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(ApiResponse.error(e.getErrorCode(), "Number of guests exceeds room capacity"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(ApiResponse.error(e.getErrorCode(), "Operation not allowed for current reservation status"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(ApiResponse.error(e.getErrorCode(), "Operation not allowed for current reservation status"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(ApiResponse.error(e.getErrorCode(), "Operation not allowed for current reservation status"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            String receipt = documentService.generateReceipt(reservation);
            ctx.json(toDocumentDto(reservation, receipt));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(ApiResponse.error(e.getErrorCode(), "Operation not allowed for current reservation status"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.isPaid()) {
                ctx.status(409).json(ApiResponse.error("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            String receipt = documentService.generateReceipt(reservation);
            ctx.json(toDocumentDto(reservation, receipt));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(ApiResponse.error(e.getErrorCode(), "Reservation not found"));
        }
    }

    private void validateRequest(CreateReservationRequest req) {
        if (req.roomNumber() == null) {
            throw new IllegalArgumentException("roomNumber is required");
        }
        if (req.guestName() == null || req.guestName().isBlank()) {
            throw new IllegalArgumentException("guestName is required");
        }
        if (req.guestEmail() == null || req.guestEmail().isBlank()) {
            throw new IllegalArgumentException("guestEmail is required");
        }
        if (req.guestCpf() == null || req.guestCpf().isBlank()) {
            throw new IllegalArgumentException("guestCpf is required");
        }
        if (req.checkIn() == null || req.checkIn().isBlank()) {
            throw new IllegalArgumentException("checkIn is required");
        }
        if (req.checkOut() == null || req.checkOut().isBlank()) {
            throw new IllegalArgumentException("checkOut is required");
        }
        if (req.guests() == null) {
            throw new IllegalArgumentException("guests is required");
        }
        if (req.ratePlan() == null || req.ratePlan().isBlank()) {
            throw new IllegalArgumentException("ratePlan is required");
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }

    private ReservationDto toDto(Reservation reservation) {
        return new ReservationDto(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }

    private DocumentDto toDocumentDto(Reservation reservation, String receipt) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        String[] lines = receipt.split("\n");
        String title = lines[0].substring(0, lines[0].lastIndexOf(" nº"));

        return new DocumentDto(
            title,
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

    record CreateReservationRequest(
        @JsonProperty("roomNumber") Integer roomNumber,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestEmail") String guestEmail,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("checkIn") String checkIn,
        @JsonProperty("checkOut") String checkOut,
        @JsonProperty("guests") Integer guests,
        @JsonProperty("ratePlan") String ratePlan
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ReservationDto(
        long id,
        @JsonProperty("roomNumber") int roomNumber,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestEmail") String guestEmail,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("checkIn") LocalDate checkIn,
        @JsonProperty("checkOut") LocalDate checkOut,
        int guests,
        @JsonProperty("ratePlan") String ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String status,
        @JsonProperty("amountPaid") BigDecimal amountPaid,
        BigDecimal refund
    ) {}

    record DocumentDto(
        String title,
        @JsonProperty("reservationId") long reservationId,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("roomNumber") int roomNumber,
        @JsonProperty("roomType") String roomType,
        @JsonProperty("checkIn") LocalDate checkIn,
        @JsonProperty("checkOut") LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    ) {}
}
