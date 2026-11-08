package br.com.pousada.controller;

import br.com.pousada.model.RatePlan;
import br.com.pousada.model.Reservation;
import br.com.pousada.model.ReservationDocument;
import br.com.pousada.service.ReservationService;
import br.com.pousada.service.ApiException;
import br.com.pousada.util.DateUtil;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

public class ReservationController {
    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);
    private static final ReservationService service = new ReservationService();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void routes(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());

                // Validar campos obrigatórios
                if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                    !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                    !body.has("guests") || !body.has("ratePlan")) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Campos obrigatórios faltando"));
                    return;
                }

                try {
                    int roomNumber = body.get("roomNumber").asInt();
                    String guestName = body.get("guestName").asText();
                    String guestEmail = body.get("guestEmail").asText();
                    String guestCpf = body.get("guestCpf").asText();
                    LocalDate checkIn = DateUtil.parseIsoDate(body.get("checkIn").asText());
                    LocalDate checkOut = DateUtil.parseIsoDate(body.get("checkOut").asText());
                    int guests = body.get("guests").asInt();
                    RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").asText());

                    Reservation reservation = service.createReservation(roomNumber, guestName, guestEmail,
                                                                        guestCpf, checkIn, checkOut, guests, ratePlan);
                    ctx.status(201);
                    ctx.json(reservation);
                } catch (IllegalArgumentException e) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de datas inválido ou plano de tarifa desconhecido"));
                }
            } catch (Exception e) {
                logger.error("Erro ao processar JSON", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Corpo da requisição inválido"));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Optional<Reservation> reservation = service.getReservation(id);
                if (reservation.isEmpty()) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                } else {
                    ctx.json(reservation.get());
                }
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                JsonNode body = mapper.readTree(ctx.body());

                if (!body.has("amountPaid")) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Campo amountPaid é obrigatório"));
                    return;
                }

                double amountPaid = body.get("amountPaid").asDouble();
                Reservation reservation = service.confirmReservation(id, amountPaid);
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            } catch (Exception e) {
                logger.error("Erro ao processar JSON", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Corpo da requisição inválido"));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = service.cancelReservation(id);
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = service.checkIn(id);
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = service.checkOut(id);

                ReservationDocument doc = new ReservationDocument(
                    reservation.id,
                    reservation.guestName,
                    reservation.guestCpf,
                    reservation.roomNumber,
                    getQuartoType(reservation.roomNumber),
                    reservation.checkIn,
                    reservation.checkOut,
                    reservation.nights,
                    reservation.subtotal,
                    reservation.discount,
                    reservation.total
                );

                ctx.json(doc);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Optional<Reservation> opt = service.getReservation(id);
                if (opt.isEmpty()) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                    return;
                }

                Reservation reservation = opt.get();
                ReservationDocument doc = new ReservationDocument(
                    reservation.id,
                    reservation.guestName,
                    reservation.guestCpf,
                    reservation.roomNumber,
                    getQuartoType(reservation.roomNumber),
                    reservation.checkIn,
                    reservation.checkOut,
                    reservation.nights,
                    reservation.subtotal,
                    reservation.discount,
                    reservation.total
                );

                ctx.json(doc);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
            }
        });

        // Exception handler para ApiException
        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.statusCode);
            ctx.json(new ErrorResponse(e.errorCode, e.getMessage()));
        });
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    private static String getQuartoType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
