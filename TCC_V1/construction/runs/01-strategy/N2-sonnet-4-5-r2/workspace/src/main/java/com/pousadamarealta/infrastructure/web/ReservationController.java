package com.pousadamarealta.infrastructure.web;

import com.pousadamarealta.domain.model.RatePlanType;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationController(ReservationService reservationService,
                                 RoomService roomService,
                                 MessageService messageService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = getRequiredString(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = parseDate(getRequiredString(body, "checkIn"));
            LocalDate checkOut = parseDate(getRequiredString(body, "checkOut"));
            int guests = getRequiredInt(body, "guests");
            RatePlanType ratePlan = RatePlanType.valueOf(getRequiredString(body, "ratePlan"));
            String companyCode = (String) body.get("companyCode");

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode
            );

            ctx.status(201).json(toReservationDto(reservation));

        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                e.getMessage()
            ));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.ROOM_NOT_FOUND.name(),
                "Quarto não encontrado"
            ));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse(
                ErrorCode.ROOM_UNAVAILABLE.name(),
                "Quarto indisponível para as datas selecionadas"
            ));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse(
                ErrorCode.CAPACITY_EXCEEDED.name(),
                "Número de hóspedes excede a capacidade do quarto"
            ));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                e.getMessage()
            ));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toReservationDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Reserva não encontrada"
            ));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toReservationDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Reserva não encontrada"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse(
                ErrorCode.OPERATION_NOT_ALLOWED.name(),
                "Operação não permitida para o status atual da reserva"
            ));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toReservationDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Reserva não encontrada"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse(
                ErrorCode.OPERATION_NOT_ALLOWED.name(),
                "Operação não permitida para o status atual da reserva"
            ));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toReservationDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Reserva não encontrada"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse(
                ErrorCode.OPERATION_NOT_ALLOWED.name(),
                "Operação não permitida para o status atual da reserva"
            ));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(toDocumentDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Reserva não encontrada"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse(
                ErrorCode.OPERATION_NOT_ALLOWED.name(),
                "Operação não permitida para o status atual da reserva"
            ));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.OPERATION_NOT_ALLOWED.name(),
                    "Documento disponível apenas para reservas pagas"
                ));
                return;
            }

            ctx.json(toDocumentDto(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Reserva não encontrada"
            ));
        }
    }

    private Map<String, Object> toReservationDto(Reservation reservation) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", reservation.getId());
        dto.put("roomNumber", reservation.getRoomNumber());
        dto.put("guestName", reservation.getGuestName());
        dto.put("guestEmail", reservation.getGuestEmail());
        dto.put("guestCpf", reservation.getGuestCpf());
        dto.put("checkIn", reservation.getCheckIn().format(DATE_FORMATTER));
        dto.put("checkOut", reservation.getCheckOut().format(DATE_FORMATTER));
        dto.put("guests", reservation.getGuests());
        dto.put("ratePlan", reservation.getRatePlan().name());
        dto.put("nights", reservation.getNights());
        dto.put("subtotal", reservation.getSubtotal());
        dto.put("discount", reservation.getDiscount());
        dto.put("total", reservation.getTotal());
        dto.put("status", reservation.getStatus().name());
        dto.put("amountPaid", reservation.getAmountPaid());
        dto.put("refund", reservation.getRefund());
        return dto;
    }

    private Map<String, Object> toDocumentDto(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        Map<String, Object> dto = new HashMap<>();
        dto.put("title", "Comprovante de reserva");
        dto.put("reservationId", reservation.getId());
        dto.put("guestName", reservation.getGuestName());
        dto.put("guestCpf", reservation.getGuestCpf());
        dto.put("roomNumber", reservation.getRoomNumber());
        dto.put("roomType", room.getType().name());
        dto.put("checkIn", reservation.getCheckIn().format(DATE_FORMATTER));
        dto.put("checkOut", reservation.getCheckOut().format(DATE_FORMATTER));
        dto.put("nights", reservation.getNights());
        dto.put("subtotal", reservation.getSubtotal());
        dto.put("discount", reservation.getDiscount());
        dto.put("total", reservation.getTotal());
        return dto;
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + key);
        }
        return value.toString();
    }

    private int getRequiredInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido para " + key);
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data inválido. Use yyyy-MM-dd");
        }
    }
}
