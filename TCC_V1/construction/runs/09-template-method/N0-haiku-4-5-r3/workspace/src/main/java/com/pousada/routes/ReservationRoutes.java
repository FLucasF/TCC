package com.pousada.routes;

import com.pousada.database.Database;
import com.pousada.dto.ConfirmReservationRequest;
import com.pousada.dto.CreateReservationRequest;
import com.pousada.dto.ReservationDTO;
import com.pousada.dto.VoucherDTO;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.services.MessageService;
import com.pousada.services.ReservationService;
import com.pousada.services.RoomService;
import com.pousada.services.VoucherService;
import com.pousada.utils.ErrorResponse;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationRoutes {
    private final ReservationService reservationService;
    private final RoomService roomService;
    private final MessageService messageService;
    private final VoucherService voucherService;

    public ReservationRoutes(Database db) {
        this.reservationService = new ReservationService(db);
        this.roomService = new RoomService(db);
        this.messageService = new MessageService(db);
        this.voucherService = new VoucherService(roomService);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest body = ctx.bodyAsClass(CreateReservationRequest.class);

            // Validação de campos obrigatórios
            if (body.roomNumber == 0 || body.guestName == null || body.guestEmail == null ||
                    body.guestCpf == null || body.checkIn == null || body.checkOut == null ||
                    body.guests == 0 || body.ratePlan == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Faltam campos obrigatórios"));
                return;
            }

            int roomNumber = body.roomNumber;
            String guestName = body.guestName;
            String guestEmail = body.guestEmail;
            String guestCpf = body.guestCpf;
            LocalDate checkIn = body.checkIn;
            LocalDate checkOut = body.checkOut;
            int guests = body.guests;
            String ratePlan = body.ratePlan;

            // Validação de quarto
            Room room = roomService.getRoomByNumber(roomNumber);
            if (room == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Quarto não encontrado"));
                return;
            }

            // Validação de datas
            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Data de saída deve ser após data de entrada"));
                return;
            }

            // Validação de hóspedes
            if (guests < 1) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Número de hóspedes deve ser pelo menos 1"));
                return;
            }

            if (guests > room.capacity) {
                ctx.status(422);
                ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto"));
                return;
            }

            // Validação de disponibilidade
            if (!reservationService.isRoomAvailable(roomNumber, checkIn, checkOut)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Quarto não está disponível para as datas selecionadas"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, room);

            ctx.status(201);
            ctx.json(new ReservationDTO(reservation));
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de data inválido, use yyyy-MM-dd"));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Requisição inválida: " + e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            ctx.json(new ReservationDTO(reservation));
        } catch (Exception e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            ConfirmReservationRequest body = ctx.bodyAsClass(ConfirmReservationRequest.class);

            if (body.amountPaid == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Campo amountPaid é obrigatório"));
                return;
            }

            Reservation reservation = reservationService.getReservationById(id);
            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            if (!"PENDING".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Só é possível confirmar reservas pendentes"));
                return;
            }

            BigDecimal amountPaid = body.amountPaid;
            Reservation confirmed = reservationService.confirmReservation(id, amountPaid);

            // Enviar email
            String emailBody = voucherService.generateConfirmationEmail(confirmed);
            messageService.sendMessage("EMAIL", confirmed.guestEmail,
                    "Reserva confirmada - Pousada Maré Alta", emailBody);

            ctx.json(new ReservationDTO(confirmed));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Requisição inválida: " + e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            if ("CHECKED_IN".equals(reservation.status) || "CHECKED_OUT".equals(reservation.status) ||
                    "CANCELLED".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cancelamento não permitido neste status"));
                return;
            }

            Reservation cancelled = reservationService.cancelReservation(id, reservation.ratePlan,
                    reservation.amountPaid);

            ctx.json(new ReservationDTO(cancelled));
        } catch (Exception e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            if (!"CONFIRMED".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Só é possível fazer check-in de reservas confirmadas"));
                return;
            }

            Reservation checkedIn = reservationService.checkIn(id);
            ctx.json(new ReservationDTO(checkedIn));
        } catch (Exception e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            if (!"CHECKED_IN".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Só é possível fazer check-out de reservas com check-in"));
                return;
            }

            Reservation checkedOut = reservationService.checkOut(id);
            Room room = roomService.getRoomByNumber(checkedOut.roomNumber);

            VoucherDTO voucher = new VoucherDTO(
                    checkedOut.id,
                    checkedOut.guestName,
                    checkedOut.guestCpf,
                    room.number,
                    room.type,
                    checkedOut.checkIn,
                    checkedOut.checkOut,
                    checkedOut.nights,
                    checkedOut.subtotal,
                    checkedOut.discount,
                    checkedOut.total
            );

            ctx.json(voucher);
        } catch (Exception e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            if ("PENDING".equals(reservation.status) || "CANCELLED".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Documento não disponível para este status"));
                return;
            }

            Room room = roomService.getRoomByNumber(reservation.roomNumber);

            VoucherDTO voucher = new VoucherDTO(
                    reservation.id,
                    reservation.guestName,
                    reservation.guestCpf,
                    room.number,
                    room.type,
                    reservation.checkIn,
                    reservation.checkOut,
                    reservation.nights,
                    reservation.subtotal,
                    reservation.discount,
                    reservation.total
            );

            ctx.json(voucher);
        } catch (Exception e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
        }
    }
}
