package com.pousadamarealta.api;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationDocument;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.SentMessage;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.pousadamarealta.service.ReservationService.*;

public final class ApiController {
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final MessageRepository messageRepository;
    private final ReportService reportService;

    public ApiController(
        RoomRepository roomRepository,
        ReservationService reservationService,
        MessageRepository messageRepository,
        ReportService reportService
    ) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.messageRepository = messageRepository;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms.stream().map(RoomResponse::from).toList());
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.roomNumber() == null || request.guestName() == null ||
                request.guestEmail() == null || request.guestCpf() == null ||
                request.checkIn() == null || request.checkOut() == null ||
                request.ratePlan() == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                request.ratePlan()
            );

            ctx.status(201).json(ReservationResponse.from(reservation));
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirm(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm reservation in current status"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancel(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current status"));
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check in reservation in current status"));
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ReservationDocument document = reservationService.createDocument(reservation);
            ctx.json(DocumentResponse.from(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check out reservation in current status"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            ReservationDocument document = reservationService.createDocument(reservation);
            ctx.json(DocumentResponse.from(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void getMessages(Context ctx) {
        List<SentMessage> messages = messageRepository.findAll();
        ctx.json(messages.stream().map(MessageResponse::from).toList());
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
