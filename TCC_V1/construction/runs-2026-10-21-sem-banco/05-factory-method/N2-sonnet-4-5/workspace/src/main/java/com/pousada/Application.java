package com.pousada;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.dto.*;
import com.pousada.domain.*;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Application {

    public static void main(String[] args) {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        DocumentService documentService = new DocumentService();
        EmailService emailService = new EmailService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, emailService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll().stream()
                .map(r -> new RoomResponse(r.getNumber(), r.getType().name(), r.getCapacity(), r.getNightlyRate()))
                .toList();
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

                if (request.getRoomNumber() == null || request.getGuestName() == null ||
                    request.getGuestEmail() == null || request.getCheckIn() == null ||
                    request.getCheckOut() == null || request.getGuests() == null ||
                    request.getRatePlan() == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                if (request.getCheckOut().isBefore(request.getCheckIn()) ||
                    request.getCheckOut().isEqual(request.getCheckIn())) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                    return;
                }

                GuestIdentification identification = parseIdentification(request);
                if (identification == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid guest identification"));
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
                    identification,
                    request.getCheckIn(),
                    request.getCheckOut(),
                    request.getGuests(),
                    ratePlan
                );

                ctx.status(201).json(ReservationResponse.from(reservation));

            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
            } catch (ReservationService.InvalidRequestException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request: " + e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current state"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current state"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current state"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                String documentText = documentService.generateDocument(reservation);
                ctx.json(DocumentResponse.from(reservation, documentText));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current state"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation.getStatus() == ReservationStatus.PENDING) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document not available for pending reservations"));
                    return;
                }

                String documentText = documentService.generateDocument(reservation);
                ctx.json(DocumentResponse.from(reservation, documentText));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/messages", ctx -> {
            var messages = emailService.getAllMessages().stream()
                .map(MessageResponse::from)
                .toList();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                String[] parts = monthStr.split("-");
                if (parts.length != 2) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                    return;
                }

                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                if (month < 1 || month > 12) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month"));
                    return;
                }

                String csv = reportService.generateRevenueReport(year, month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }

    private static GuestIdentification parseIdentification(CreateReservationRequest request) {
        int count = 0;
        GuestIdentification result = null;

        if (request.getGuestCpf() != null) {
            count++;
            result = new CpfIdentification(request.getGuestCpf());
        }

        if (request.getCompanyCnpj() != null && request.getCompanyName() != null) {
            count++;
            result = new CnpjIdentification(request.getCompanyCnpj(), request.getCompanyName());
        } else if (request.getCompanyCnpj() != null || request.getCompanyName() != null) {
            return null;
        }

        if (request.getGuestPassport() != null && request.getGuestCountry() != null) {
            count++;
            result = new PassportIdentification(request.getGuestPassport(), request.getGuestCountry());
        } else if (request.getGuestPassport() != null || request.getGuestCountry() != null) {
            return null;
        }

        return count == 1 ? result : null;
    }
}
