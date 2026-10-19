package com.pousadamareaalta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamareaalta.model.*;
import com.pousadamareaalta.service.*;
import com.pousadamareaalta.util.DateUtils;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ApiHandlers {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageService messageService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ApiHandlers(
        ReservationService reservationService,
        RoomRepository roomRepository,
        ReservationRepository reservationRepository,
        MessageService messageService,
        ReportService reportService
    ) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageService = messageService;
        this.reportService = reportService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void getRooms(Context ctx) {
        var rooms = roomRepository.findAll().stream()
            .map(RoomResponse::from)
            .toList();
        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            if (req.roomNumber() == null || req.guestName() == null || req.guestEmail() == null ||
                req.checkIn() == null || req.checkOut() == null || req.guests() == null ||
                req.ratePlan() == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Campos obrigatórios ausentes"));
                return;
            }

            var checkIn = DateUtils.parseDate(req.checkIn());
            var checkOut = DateUtils.parseDate(req.checkOut());
            var ratePlan = RatePlan.valueOf(req.ratePlan());

            Reservation reservation = reservationService.createReservation(
                req.roomNumber(),
                req.guestName(),
                req.guestEmail(),
                req.guestCpf(),
                req.companyCnpj(),
                req.companyName(),
                req.guestPassport(),
                req.guestCountry(),
                checkIn,
                checkOut,
                req.guests(),
                ratePlan
            );

            ctx.status(201);
            ctx.json(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de dados inválido"));
        } catch (ReservationService.ValidationException e) {
            int status = switch (e.errorCode) {
                case "ROOM_NOT_FOUND" -> 404;
                case "ROOM_UNAVAILABLE" -> 409;
                case "CAPACITY_EXCEEDED" -> 422;
                case "OPERATION_NOT_ALLOWED" -> 409;
                default -> 400;
            };
            ctx.status(status);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao processar reserva"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);
            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = switch (e.errorCode) {
                case "RESERVATION_NOT_FOUND" -> 404;
                case "OPERATION_NOT_ALLOWED" -> 409;
                default -> 400;
            };
            ctx.status(status);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao confirmar reserva"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = switch (e.errorCode) {
                case "RESERVATION_NOT_FOUND" -> 404;
                case "OPERATION_NOT_ALLOWED" -> 409;
                default -> 400;
            };
            ctx.status(status);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao cancelar reserva"));
        }
    }

    public void checkin(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkin(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = switch (e.errorCode) {
                case "RESERVATION_NOT_FOUND" -> 404;
                case "OPERATION_NOT_ALLOWED" -> 409;
                default -> 400;
            };
            ctx.status(status);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao fazer check-in"));
        }
    }

    public void checkout(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.checkout(id);
            ctx.json(DocumentResponse.from(document));
        } catch (ReservationService.ValidationException e) {
            int status = switch (e.errorCode) {
                case "RESERVATION_NOT_FOUND" -> 404;
                case "OPERATION_NOT_ALLOWED" -> 409;
                default -> 400;
            };
            ctx.status(status);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao fazer check-out"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.getDocument(id);
            ctx.json(DocumentResponse.from(document));
        } catch (ReservationService.ValidationException e) {
            int status = switch (e.errorCode) {
                case "RESERVATION_NOT_FOUND" -> 404;
                case "OPERATION_NOT_ALLOWED" -> 409;
                default -> 400;
            };
            ctx.status(status);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao gerar documento"));
        }
    }

    public void getMessages(Context ctx) {
        var messages = messageService.findAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String date = ctx.queryParam("date");
            if (date == null || date.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro date é obrigatório"));
                return;
            }
            var parsedDate = DateUtils.parseDate(date);
            String report = reportService.generateOccupancyReport(parsedDate);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao gerar relatório de ocupação"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String month = ctx.queryParam("month");
            if (month == null || month.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro month é obrigatório"));
                return;
            }
            String report = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (ReservationService.ValidationException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Erro ao gerar relatório de receita"));
        }
    }
}
