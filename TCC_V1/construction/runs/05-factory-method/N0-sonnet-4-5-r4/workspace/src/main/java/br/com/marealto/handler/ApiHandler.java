package br.com.marealto.handler;

import br.com.marealto.dto.*;
import br.com.marealto.model.Message;
import br.com.marealto.model.Reservation;
import br.com.marealto.model.ReservationStatus;
import br.com.marealto.model.Room;
import br.com.marealto.repository.MessageRepository;
import br.com.marealto.repository.RoomRepository;
import br.com.marealto.service.DocumentService;
import br.com.marealto.service.ReportService;
import br.com.marealto.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class ApiHandler {
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final DocumentService documentService;
    private final ReportService reportService;

    public ApiHandler(
            RoomRepository roomRepository,
            MessageRepository messageRepository,
            ReservationService reservationService,
            DocumentService documentService,
            ReportService reportService) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.reservationService = reservationService;
        this.documentService = documentService;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<RoomResponse> response = rooms.stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
        ctx.json(response);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
            request.validate();

            Reservation reservation = reservationService.createReservation(request.toReservation());
            ReservationResponse response = ReservationResponse.from(reservation);

            ctx.status(201).json(response);
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ReservationResponse response = ReservationResponse.from(reservation);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ReservationResponse response = ReservationResponse.from(reservation);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ReservationResponse response = ReservationResponse.from(reservation);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ReservationResponse response = ReservationResponse.from(reservation);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            String documentText = documentService.generateDocument(reservation, room.getType());
            DocumentResponse response = DocumentResponse.from(reservation, room.getType(), documentText);

            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus() == ReservationStatus.PENDING) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED",
                    "Document is only available for paid reservations"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            String documentText = documentService.generateDocument(reservation, room.getType());
            DocumentResponse response = DocumentResponse.from(reservation, room.getType(), documentText);

            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<MessageResponse> response = messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
        ctx.json(response);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
