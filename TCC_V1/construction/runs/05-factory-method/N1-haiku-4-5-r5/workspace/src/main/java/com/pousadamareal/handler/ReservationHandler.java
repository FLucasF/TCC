package com.pousadamareal.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pousadamareal.model.*;
import com.pousadamareal.repository.RoomRepository;
import com.pousadamareal.service.DocumentGenerator;
import com.pousadamareal.service.ReservationService;
import com.pousadamareal.util.JsonUtils;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class ReservationHandler {

    public static void create(Context ctx) {
        try {
            String body = ctx.body();
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            if (!json.has("roomNumber") || !json.has("guestName") || !json.has("guestEmail") ||
                !json.has("checkIn") || !json.has("checkOut") || !json.has("guests") || !json.has("ratePlan")) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Campos obrigatórios faltam"));
                return;
            }

            int roomNumber;
            String guestName;
            String guestEmail;
            String guestCpf = null;
            String companyCnpj = null;
            String companyName = null;
            String guestPassport = null;
            String guestCountry = null;
            LocalDate checkIn;
            LocalDate checkOut;
            int guests;
            RatePlan ratePlan;

            try {
                roomNumber = json.get("roomNumber").getAsInt();
                guestName = json.get("guestName").getAsString();
                guestEmail = json.get("guestEmail").getAsString();
                checkIn = LocalDate.parse(json.get("checkIn").getAsString());
                checkOut = LocalDate.parse(json.get("checkOut").getAsString());
                guests = json.get("guests").getAsInt();
                ratePlan = RatePlan.valueOf(json.get("ratePlan").getAsString());
            } catch (NumberFormatException | DateTimeParseException e) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Formato inválido"));
                return;
            }

            int identificationCount = 0;
            if (json.has("guestCpf")) {
                guestCpf = json.get("guestCpf").getAsString();
                identificationCount++;
            }
            if (json.has("companyCnpj")) {
                companyCnpj = json.get("companyCnpj").getAsString();
                identificationCount++;
            }
            if (json.has("guestPassport")) {
                guestPassport = json.get("guestPassport").getAsString();
                identificationCount++;
            }

            if (identificationCount != 1) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Deve informar exatamente um tipo de identificação"));
                return;
            }

            if (json.has("companyCnpj") && !json.has("companyName")) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "companyName é obrigatório com companyCnpj"));
                return;
            }

            if (json.has("guestPassport") && !json.has("guestCountry")) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "guestCountry é obrigatório com guestPassport"));
                return;
            }

            if (json.has("companyName")) {
                companyName = json.get("companyName").getAsString();
            }
            if (json.has("guestCountry")) {
                guestCountry = json.get("guestCountry").getAsString();
            }

            try {
                Reservation reservation = ReservationService.create(
                    roomNumber, guestName, guestEmail,
                    guestCpf, companyCnpj, companyName,
                    guestPassport, guestCountry,
                    checkIn, checkOut, guests, ratePlan
                );
                ctx.status(HttpStatus.CREATED).json(toJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Erro ao processar requisição"));
        }
    }

    public static void get(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getById(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "ID inválido"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public static void confirm(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            String body = ctx.body();
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            if (!json.has("amountPaid")) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "amountPaid é obrigatório"));
                return;
            }

            BigDecimal amountPaid = json.get("amountPaid").getAsBigDecimal();
            Reservation reservation = ReservationService.confirm(id, amountPaid);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "ID ou valor inválido"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Erro ao processar requisição"));
        }
    }

    public static void cancel(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.cancel(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "ID inválido"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public static void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.checkIn(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "ID inválido"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public static void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.checkOut(id);

            Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber());
            Document document = DocumentGenerator.generate(reservation, room.get());

            ctx.json(documentToJson(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "ID inválido"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public static void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getById(id);

            if (reservation.status() == ReservationStatus.PENDING || reservation.status() == ReservationStatus.CANCELLED) {
                ctx.status(409).json(JsonUtils.map("error", "OPERATION_NOT_ALLOWED", "message", "Documento não disponível"));
                return;
            }

            Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber());
            Document document = DocumentGenerator.generate(reservation, room.get());

            ctx.json(documentToJson(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "ID inválido"));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    private static void handleError(Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(JsonUtils.map("error", "ROOM_NOT_FOUND", "message", "Quarto não encontrado"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(JsonUtils.map("error", "RESERVATION_NOT_FOUND", "message", "Reserva não encontrada"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(JsonUtils.map("error", "ROOM_UNAVAILABLE", "message", "Quarto não está disponível"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(JsonUtils.map("error", "CAPACITY_EXCEEDED", "message", "Excedeu a capacidade do quarto"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(JsonUtils.map("error", "OPERATION_NOT_ALLOWED", "message", "Operação não permitida"));
                break;
            case "INVALID_REQUEST":
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Requisição inválida"));
                break;
            default:
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", error));
        }
    }

    private static java.util.Map<String, Object> toJson(Reservation reservation) {
        return JsonUtils.map(
            "id", reservation.id(),
            "roomNumber", reservation.roomNumber(),
            "guestName", reservation.guestName(),
            "guestEmail", reservation.guestEmail(),
            "guestCpf", reservation.guestCpf(),
            "companyCnpj", reservation.companyCnpj(),
            "companyName", reservation.companyName(),
            "guestPassport", reservation.guestPassport(),
            "guestCountry", reservation.guestCountry(),
            "checkIn", reservation.checkIn().toString(),
            "checkOut", reservation.checkOut().toString(),
            "guests", reservation.guests(),
            "ratePlan", reservation.ratePlan().name(),
            "nights", reservation.nights(),
            "subtotal", reservation.subtotal(),
            "discount", reservation.discount(),
            "total", reservation.total(),
            "status", reservation.status().name(),
            "amountPaid", reservation.amountPaid(),
            "refund", reservation.refund()
        );
    }

    private static java.util.Map<String, Object> documentToJson(Document document) {
        return JsonUtils.map(
            "title", document.title(),
            "reservationId", document.reservationId(),
            "guestName", document.guestName(),
            "guestCpf", document.guestCpf(),
            "companyCnpj", document.companyCnpj(),
            "companyName", document.companyName(),
            "guestPassport", document.guestPassport(),
            "guestCountry", document.guestCountry(),
            "roomNumber", document.roomNumber(),
            "roomType", document.roomType(),
            "checkIn", document.checkIn(),
            "checkOut", document.checkOut(),
            "nights", document.nights(),
            "subtotal", document.subtotal(),
            "discount", document.discount(),
            "total", document.total(),
            "text", document.text()
        );
    }
}
