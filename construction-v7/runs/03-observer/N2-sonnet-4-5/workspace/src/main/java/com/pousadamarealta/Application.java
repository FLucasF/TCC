package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.observer.*;
import com.pousadamarealta.service.EmailService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

public class Application {
    public static void main(String[] args) {
        EmailService emailService = new EmailService();
        PricingService pricingService = new PricingService();
        ReservationEventPublisher eventPublisher = new ReservationEventPublisher();
        ReservationService reservationService = new ReservationService(pricingService, eventPublisher);
        ReportService reportService = new ReportService(reservationService);

        eventPublisher.attach(new GuestNotificationObserver(emailService, reservationService));
        eventPublisher.attach(new HousekeepingNotificationObserver(emailService));
        eventPublisher.attach(new FinanceNotificationObserver(emailService));

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", ctx -> {
            var rooms = reservationService.getAllRooms().stream()
                    .map(RoomResponse::from)
                    .collect(Collectors.toList());
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
                validateCreateReservationRequest(request);

                LocalDate checkIn = LocalDate.parse(request.getCheckIn());
                LocalDate checkOut = LocalDate.parse(request.getCheckOut());

                if (!checkOut.isAfter(checkIn)) {
                    throw new ApiException("INVALID_REQUEST", "A data de saída deve ser após a data de entrada", 400);
                }

                Reservation reservation = reservationService.createReservation(
                        request.getRoomNumber(),
                        request.getGuestName(),
                        request.getGuestEmail(),
                        request.getGuestCpf(),
                        checkIn,
                        checkOut,
                        request.getGuests(),
                        request.getRatePlan()
                );

                ctx.status(HttpStatus.CREATED);
                ctx.json(ReservationResponse.from(reservation));
            } catch (DateTimeParseException e) {
                throw new ApiException("INVALID_REQUEST", "Formato de data inválido. Use yyyy-MM-dd", 400);
            } catch (NullPointerException e) {
                throw new ApiException("INVALID_REQUEST", "Campos obrigatórios faltando", 400);
            }
        });

        app.get("/reservations/{id}", ctx -> {
            long id = parseLongOrThrow(ctx, "id");
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            long id = parseLongOrThrow(ctx, "id");
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            long id = parseLongOrThrow(ctx, "id");
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            long id = parseLongOrThrow(ctx, "id");
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            long id = parseLongOrThrow(ctx, "id");
            Reservation reservation = reservationService.checkOut(id);
            String roomType = reservationService.getRoom(reservation.getRoomNumber()).getType().name();
            ctx.json(ReceiptResponse.from(reservation, roomType));
        });

        app.get("/reservations/{id}/document", ctx -> {
            long id = parseLongOrThrow(ctx, "id");
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().signum() == 0) {
                throw new ApiException("OPERATION_NOT_ALLOWED",
                        "Apenas reservas pagas têm comprovante", 409);
            }

            String roomType = reservationService.getRoom(reservation.getRoomNumber()).getType().name();
            ctx.json(ReceiptResponse.from(reservation, roomType));
        });

        app.get("/messages", ctx -> {
            var messages = emailService.getMessages().stream()
                    .map(MessageResponse::from)
                    .collect(Collectors.toList());
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                throw new ApiException("INVALID_REQUEST", "Parâmetro 'date' é obrigatório", 400);
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                throw new ApiException("INVALID_REQUEST", "Formato de data inválido. Use yyyy-MM-dd", 400);
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                throw new ApiException("INVALID_REQUEST", "Parâmetro 'month' é obrigatório", 400);
            }

            try {
                YearMonth month = YearMonth.parse(monthStr);
                String csv = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                throw new ApiException("INVALID_REQUEST", "Formato de mês inválido. Use yyyy-MM", 400);
            }
        });

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });
    }

    private static void validateCreateReservationRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getRoomNumber().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "roomNumber é obrigatório", 400);
        }
        if (request.getGuestName() == null || request.getGuestName().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "guestName é obrigatório", 400);
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "guestEmail é obrigatório", 400);
        }
        if (request.getGuestCpf() == null || request.getGuestCpf().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "guestCpf é obrigatório", 400);
        }
        if (request.getCheckIn() == null || request.getCheckIn().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "checkIn é obrigatório", 400);
        }
        if (request.getCheckOut() == null || request.getCheckOut().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "checkOut é obrigatório", 400);
        }
        if (request.getGuests() == null) {
            throw new ApiException("INVALID_REQUEST", "guests é obrigatório", 400);
        }
        if (request.getRatePlan() == null) {
            throw new ApiException("INVALID_REQUEST", "ratePlan é obrigatório", 400);
        }
    }

    private static long parseLongOrThrow(Context ctx, String param) {
        try {
            return Long.parseLong(ctx.pathParam(param));
        } catch (NumberFormatException e) {
            throw new ApiException("INVALID_REQUEST", "ID inválido", 400);
        }
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("PORT inválida, usando 7070");
            }
        }
        return 7070;
    }
}
