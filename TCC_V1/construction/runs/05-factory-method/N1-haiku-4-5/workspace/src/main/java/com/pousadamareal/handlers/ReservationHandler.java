package com.pousadamareal.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pousadamareal.dao.MessageDAO;
import com.pousadamareal.dao.ReservationDAO;
import com.pousadamareal.dao.RoomDAO;
import com.pousadamareal.model.*;
import com.pousadamareal.util.DocumentFormatter;
import com.pousadamareal.util.PriceCalculator;
import io.javalin.http.Context;

import java.time.LocalDate;

public class ReservationHandler {
    public static void createReservation(Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("checkIn") || !body.has("checkOut") || !body.has("guests") || !body.has("ratePlan")) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Campos obrigatórios faltando"));
                ctx.status(400);
                return;
            }

            String cpf = body.has("guestCpf") ? body.get("guestCpf").getAsString() : null;
            String cnpj = body.has("companyCnpj") ? body.get("companyCnpj").getAsString() : null;
            String companyName = body.has("companyName") ? body.get("companyName").getAsString() : null;
            String passport = body.has("guestPassport") ? body.get("guestPassport").getAsString() : null;
            String country = body.has("guestCountry") ? body.get("guestCountry").getAsString() : null;

            int identificationCount = (cpf != null ? 1 : 0) + (cnpj != null ? 1 : 0) + (passport != null ? 1 : 0);
            if (identificationCount != 1) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Exatamente um tipo de identificação deve ser fornecido"));
                ctx.status(400);
                return;
            }

            if (cnpj != null && companyName == null) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "companyName obrigatório quando companyCnpj é fornecido"));
                ctx.status(400);
                return;
            }

            if (passport != null && country == null) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "guestCountry obrigatório quando guestPassport é fornecido"));
                ctx.status(400);
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String checkIn = body.get("checkIn").getAsString();
            String checkOut = body.get("checkOut").getAsString();
            int guests = body.get("guests").getAsInt();
            String ratePlan = body.get("ratePlan").getAsString();

            if (guests < 1) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Número de hóspedes deve ser pelo menos 1"));
                ctx.status(400);
                return;
            }

            try {
                LocalDate.parse(checkIn);
                LocalDate.parse(checkOut);
            } catch (Exception e) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Datas inválidas (formato: yyyy-MM-dd)"));
                ctx.status(400);
                return;
            }

            Room room = RoomDAO.getRoomByNumber(roomNumber);
            if (room == null) {
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Quarto não encontrado"));
                ctx.status(404);
                return;
            }

            if (guests > room.capacity()) {
                ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto"));
                ctx.status(422);
                return;
            }

            if (!ReservationDAO.isRoomAvailableForDates(roomNumber, checkIn, checkOut)) {
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Quarto não disponível para as datas solicitadas"));
                ctx.status(409);
                return;
            }

            LocalDate checkInDate = LocalDate.parse(checkIn);
            LocalDate checkOutDate = LocalDate.parse(checkOut);
            int nights = PriceCalculator.countNights(checkInDate, checkOutDate);

            double nightlyRate = PriceCalculator.calculateNightlyRate(checkInDate, room.nightlyRate());
            double subtotal = PriceCalculator.calculateSubtotal(nights, nightlyRate);
            double discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
            double total = PriceCalculator.calculateTotal(subtotal, discount);

            Reservation reservation = ReservationDAO.createReservation(
                String.valueOf(roomNumber), guestName, guestEmail,
                cpf, cnpj, companyName, passport, country,
                checkIn, checkOut, guests, ratePlan, nights,
                subtotal, discount, total
            );

            ctx.json(reservation);
            ctx.status(201);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationDAO.getReservationById(id);

            if (reservation == null) {
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.status(404);
                return;
            }

            ctx.json(reservation);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationDAO.getReservationById(id);

            if (reservation == null) {
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.status(404);
                return;
            }

            if (reservation.status != ReservationStatus.PENDING) {
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Apenas reservas pendentes podem ser confirmadas"));
                ctx.status(409);
                return;
            }

            ReservationDAO.updateReservationStatus(id, "CONFIRMED", reservation.total, null);
            reservation = ReservationDAO.getReservationById(id);

            Room room = RoomDAO.getRoomByNumber(reservation.roomNumber);
            String documentText = generateDocument(reservation, room).text;

            String emailBody = DocumentFormatter.generateConfirmationEmail(reservation.guestName, documentText);
            MessageDAO.saveMessage("EMAIL", reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);

            ctx.json(reservation);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationDAO.getReservationById(id);

            if (reservation == null) {
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.status(404);
                return;
            }

            if (reservation.status != ReservationStatus.PENDING && reservation.status != ReservationStatus.CONFIRMED) {
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cancelamento não permitido para este status"));
                ctx.status(409);
                return;
            }

            double refund = 0;
            if (reservation.status == ReservationStatus.CONFIRMED && reservation.ratePlan == RatePlan.STANDARD) {
                refund = reservation.total;
            }

            ReservationDAO.updateReservationStatus(id, "CANCELLED", 0, refund > 0 ? refund : null);
            reservation = ReservationDAO.getReservationById(id);

            ctx.json(reservation);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void checkIn(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationDAO.getReservationById(id);

            if (reservation == null) {
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.status(404);
                return;
            }

            if (reservation.status != ReservationStatus.CONFIRMED) {
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Apenas reservas confirmadas podem fazer check-in"));
                ctx.status(409);
                return;
            }

            ReservationDAO.updateReservationStatus(id, "CHECKED_IN", reservation.amountPaid, reservation.refund);
            reservation = ReservationDAO.getReservationById(id);

            ctx.json(reservation);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void checkOut(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationDAO.getReservationById(id);

            if (reservation == null) {
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.status(404);
                return;
            }

            if (reservation.status != ReservationStatus.CHECKED_IN) {
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Apenas hóspedes com check-in podem fazer check-out"));
                ctx.status(409);
                return;
            }

            ReservationDAO.updateReservationStatus(id, "CHECKED_OUT", reservation.amountPaid, reservation.refund);
            reservation = ReservationDAO.getReservationById(id);

            Room room = RoomDAO.getRoomByNumber(reservation.roomNumber);
            Document doc = generateDocument(reservation, room);

            ctx.json(doc);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationDAO.getReservationById(id);

            if (reservation == null) {
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.status(404);
                return;
            }

            if (reservation.status == ReservationStatus.PENDING) {
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Documento só está disponível para reservas pagas"));
                ctx.status(409);
                return;
            }

            Room room = RoomDAO.getRoomByNumber(reservation.roomNumber);
            Document doc = generateDocument(reservation, room);

            ctx.json(doc);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    private static Document generateDocument(Reservation res, Room room) {
        String title;
        String text;

        if (res.guestCpf != null) {
            title = "Comprovante de reserva";
            text = DocumentFormatter.generateComprovante(res.id, res, room.type());
        } else if (res.companyCnpj != null) {
            title = "Fatura de hospedagem";
            text = DocumentFormatter.generateFatura(res.id, res, room.type());
        } else {
            title = "Booking receipt";
            text = DocumentFormatter.generateBookingReceipt(res.id, res, room.type());
        }

        return new Document(
            title, res.id, res.guestName, res.guestCpf, res.companyCnpj, res.companyName,
            res.guestPassport, res.guestCountry, res.roomNumber, room.type(),
            res.checkIn, res.checkOut, res.nights, res.subtotal, res.discount, res.total, text
        );
    }

    public record ErrorResponse(String error, String message) {}
}
