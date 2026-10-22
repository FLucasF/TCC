package com.mareaalta.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mareaalta.domain.DocumentResponse;
import com.mareaalta.domain.Reservation;
import com.mareaalta.domain.Room;
import com.mareaalta.infrastructure.RoomRepository;
import com.mareaalta.service.DocumentGenerator;
import com.mareaalta.service.ReservationService;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationController {
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void createReservation(Context ctx) throws Exception {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (body == null) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Corpo da requisição inválido"));
                return;
            }

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("checkIn") || !body.has("checkOut") || !body.has("guests") || !body.has("ratePlan")) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Campos obrigatórios faltando"));
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), ISO_DATE);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), ISO_DATE);
            int guests = body.get("guests").getAsInt();
            String ratePlan = body.get("ratePlan").getAsString();

            if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Data de saída deve ser depois da entrada"));
                return;
            }

            // Validate identification
            String guestCpf = null;
            String companyCnpj = null;
            String companyName = null;
            String guestPassport = null;
            String guestCountry = null;

            int identificationCount = 0;

            if (body.has("guestCpf") && body.get("guestCpf").isJsonPrimitive()) {
                guestCpf = body.get("guestCpf").getAsString();
                identificationCount++;
            }

            if (body.has("companyCnpj") && body.get("companyCnpj").isJsonPrimitive()) {
                companyCnpj = body.get("companyCnpj").getAsString();
                companyName = body.get("companyName").getAsString();
                identificationCount++;
            }

            if (body.has("guestPassport") && body.get("guestPassport").isJsonPrimitive()) {
                guestPassport = body.get("guestPassport").getAsString();
                guestCountry = body.get("guestCountry").getAsString();
                identificationCount++;
            }

            if (identificationCount != 1) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Exatamente um tipo de identificação é obrigatório"));
                return;
            }

            Reservation reservation = ReservationService.createReservation(
                roomNumber, guestName, guestEmail,
                guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(serializeReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = e.getCode().equals("CAPACITY_EXCEEDED") ? 422 :
                        e.getCode().equals("ROOM_NOT_FOUND") ? 404 :
                        e.getCode().equals("ROOM_UNAVAILABLE") ? 409 : 400;
            ctx.status(status).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    public static void getReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservation(id);
            ctx.json(serializeReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            ctx.status(404).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    public static void confirmReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (body == null || !body.has("amountPaid")) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Campo amountPaid obrigatório"));
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
            Reservation reservation = ReservationService.confirmReservation(id, amountPaid);
            ctx.json(serializeReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = e.getCode().equals("RESERVATION_NOT_FOUND") ? 404 : 409;
            ctx.status(status).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    public static void checkIn(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.checkIn(id);
            ctx.json(serializeReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = e.getCode().equals("RESERVATION_NOT_FOUND") ? 404 : 409;
            ctx.status(status).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    public static void checkOut(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.checkOut(id);

            Room room = RoomRepository.findByNumber(reservation.getRoomNumber());
            String documentText = ReservationService.generateDocumentText(reservation, room);

            DocumentResponse doc = buildDocumentResponse(reservation, room, documentText);
            ctx.json(doc);
        } catch (ReservationService.ValidationException e) {
            int status = e.getCode().equals("RESERVATION_NOT_FOUND") ? 404 : 409;
            ctx.status(status).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    public static void cancelReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.cancelReservation(id);
            ctx.json(serializeReservation(reservation));
        } catch (ReservationService.ValidationException e) {
            int status = e.getCode().equals("RESERVATION_NOT_FOUND") ? 404 : 409;
            ctx.status(status).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    public static void getDocument(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservation(id);

            if (reservation.getStatus() == Reservation.Status.PENDING) {
                ctx.status(409).json(errorResponse("OPERATION_NOT_ALLOWED", "Documento não disponível para reserva pendente"));
                return;
            }

            Room room = RoomRepository.findByNumber(reservation.getRoomNumber());
            String documentText = ReservationService.generateDocumentText(reservation, room);

            DocumentResponse doc = buildDocumentResponse(reservation, room, documentText);
            ctx.json(doc);
        } catch (ReservationService.ValidationException e) {
            ctx.status(404).json(errorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Erro ao processar a requisição"));
        }
    }

    private static JsonObject serializeReservation(Reservation res) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", res.getId());
        obj.addProperty("roomNumber", res.getRoomNumber());
        obj.addProperty("guestName", res.getGuestName());
        obj.addProperty("guestEmail", res.getGuestEmail());
        obj.addProperty("guestCpf", res.getGuestCpf());
        obj.addProperty("companyCnpj", res.getCompanyCnpj());
        obj.addProperty("companyName", res.getCompanyName());
        obj.addProperty("guestPassport", res.getGuestPassport());
        obj.addProperty("guestCountry", res.getGuestCountry());
        obj.addProperty("checkIn", res.getCheckIn().format(ISO_DATE));
        obj.addProperty("checkOut", res.getCheckOut().format(ISO_DATE));
        obj.addProperty("guests", res.getGuests());
        obj.addProperty("ratePlan", res.getRatePlan().name());
        obj.addProperty("nights", res.getNights());
        obj.addProperty("subtotal", res.getSubtotal().doubleValue());
        obj.addProperty("discount", res.getDiscount() != null ? res.getDiscount().doubleValue() : null);
        obj.addProperty("total", res.getTotal().doubleValue());
        obj.addProperty("status", res.getStatus().name());
        obj.addProperty("amountPaid", res.getAmountPaid().doubleValue());
        obj.addProperty("refund", res.getRefund() != null ? res.getRefund().doubleValue() : null);
        return obj;
    }

    private static DocumentResponse buildDocumentResponse(Reservation res, Room room, String text) {
        String title = res.getGuestCpf() != null ? "Comprovante de reserva" :
                      res.getCompanyCnpj() != null ? "Fatura de hospedagem" : "Booking receipt";

        return new DocumentResponse(
            title, res.getId(), res.getGuestName(),
            res.getGuestCpf(), res.getCompanyCnpj(), res.getCompanyName(),
            res.getGuestPassport(), res.getGuestCountry(),
            res.getRoomNumber(), room.getType().name(),
            res.getCheckIn().format(BR_DATE), res.getCheckOut().format(BR_DATE),
            res.getNights(), res.getSubtotal(), res.getDiscount(), res.getTotal(),
            text
        );
    }

    private static JsonObject errorResponse(String code, String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", code);
        obj.addProperty("message", message);
        return obj;
    }
}
