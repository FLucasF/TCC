package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.CreateReservationRequest;
import com.pousadamarealta.api.ErrorResponse;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        int port = getPort();
        boolean testMode = isTestMode();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        PricingService pricingService = new PricingService();
        MessageProcessor messageProcessor = createMessageProcessor(testMode);
        MessageService messageService = new MessageService(messageRepository, messageProcessor);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, pricingService, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.get("/rooms", ctx -> {
            List<Map<String, Object>> rooms = new ArrayList<>();
            for (Room room : roomRepository.findAll()) {
                Map<String, Object> roomData = new HashMap<>();
                roomData.put("number", room.getNumber());
                roomData.put("type", room.getType().name());
                roomData.put("capacity", room.getCapacity());
                roomData.put("nightlyRate", room.getNightlyRate());
                rooms.add(roomData);
            }
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

                validateNotNull(request.roomNumber(), "roomNumber");
                validateNotNull(request.guestName(), "guestName");
                validateNotNull(request.guestEmail(), "guestEmail");
                validateNotNull(request.guestCpf(), "guestCpf");
                validateNotNull(request.checkIn(), "checkIn");
                validateNotNull(request.checkOut(), "checkOut");
                validateNotNull(request.guests(), "guests");
                validateNotNull(request.ratePlan(), "ratePlan");

                LocalDate checkIn = parseDate(request.checkIn());
                LocalDate checkOut = parseDate(request.checkOut());
                RatePlan ratePlan = parseRatePlan(request.ratePlan());

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

                ctx.status(201).json(mapReservation(reservation));
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Quarto não encontrado"));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto"));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Quarto não disponível nas datas solicitadas"));
            } catch (ReservationService.InvalidRequestException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.findById(id);
                ctx.json(mapReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(mapReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operação não permitida no estado atual da reserva"));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(mapReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operação não permitida no estado atual da reserva"));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(mapReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operação não permitida no estado atual da reserva"));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Map<String, Object> document = reservationService.checkOut(id);
                ctx.json(document);
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operação não permitida no estado atual da reserva"));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Map<String, Object> document = reservationService.getDocument(id);
                ctx.json(document);
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Documento disponível apenas para reservas pagas"));
            }
        });

        app.get("/messages", ctx -> {
            List<Map<String, Object>> messages = new ArrayList<>();
            for (Message message : messageService.getAllMessages()) {
                Map<String, Object> msgData = new HashMap<>();
                msgData.put("id", message.getId());
                msgData.put("channel", message.getChannel().name());
                msgData.put("to", message.getTo());
                msgData.put("subject", message.getSubject());
                msgData.put("body", message.getBody());
                msgData.put("status", message.getStatus().name());
                messages.add(msgData);
            }
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'date' é obrigatório"));
                return;
            }

            try {
                LocalDate date = parseDate(dateParam);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Data inválida"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório"));
                return;
            }

            try {
                YearMonth month = parseMonth(monthParam);
                String csv = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Mês inválido"));
            }
        });
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }

    private static boolean isTestMode() {
        String testModeEnv = System.getenv("HOTEL_TEST_MODE");
        return "true".equalsIgnoreCase(testModeEnv);
    }

    private static MessageProcessor createMessageProcessor(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRedirectRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageProcessor(rules);
    }

    private static Map<String, Object> mapReservation(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn());
        map.put("checkOut", reservation.getCheckOut());
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

    private static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Campo obrigatório: " + fieldName);
        }
    }

    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + dateStr);
        }
    }

    private static YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mês inválido: " + monthStr);
        }
    }

    private static RatePlan parseRatePlan(String ratePlanStr) {
        try {
            return RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Plano de tarifa inválido: " + ratePlanStr);
        }
    }
}
