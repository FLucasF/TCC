package com.pousadamareaita.api;

import io.javalin.http.Context;
import com.pousadamareaita.database.Database;
import com.pousadamareaita.service.ReservationService;
import com.pousadamareaita.service.RoomService;
import com.pousadamareaita.service.MessageService;
import com.pousadamareaita.service.DocumentService;
import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.Document;
import com.pousadamareaita.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final DocumentService documentService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReservationHandler(Database db) {
        this.reservationService = new ReservationService(db);
        this.messageService = new MessageService(db);
        this.documentService = new DocumentService(new RoomService(db));
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode body = mapper.readTree(ctx.body());

            String guestName = getStringField(body, "guestName");
            String guestEmail = getStringField(body, "guestEmail");
            String guestCpf = getStringField(body, "guestCpf");
            String companyCnpj = getStringField(body, "companyCnpj");
            String companyName = getStringField(body, "companyName");
            String guestPassport = getStringField(body, "guestPassport");
            String guestCountry = getStringField(body, "guestCountry");

            if (guestName == null || guestEmail == null) {
                throw new ApiException("INVALID_REQUEST", "guestName and guestEmail are required", 400);
            }

            int roomNumber = getIntField(body, "roomNumber");
            LocalDate checkIn = getDateField(body, "checkIn");
            LocalDate checkOut = getDateField(body, "checkOut");
            int guests = getIntField(body, "guests");
            String ratePlan = getStringField(body, "ratePlan");

            if (checkIn == null || checkOut == null || ratePlan == null) {
                throw new ApiException("INVALID_REQUEST", "checkIn, checkOut, and ratePlan are required", 400);
            }

            Reservation reservation = reservationService.createReservation(
                guestName, guestEmail, guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
                roomNumber, checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201);
            ctx.json(reservation);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
            }

            ctx.json(reservation);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonNode body = mapper.readTree(ctx.body());

            BigDecimal amountPaid = null;
            if (body.has("amountPaid")) {
                amountPaid = new BigDecimal(body.get("amountPaid").asDouble()).setScale(2, java.math.RoundingMode.HALF_EVEN);
            }

            Reservation reservation = reservationService.getReservation(id);
            if (reservation == null) {
                throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
            }

            if (amountPaid == null) {
                amountPaid = reservation.getTotal();
            }

            reservationService.confirmReservation(id, amountPaid);
            reservation = reservationService.getReservation(id);

            Document document = documentService.generateDocument(reservation);

            String emailBody = "Olá, " + reservation.getGuestName() + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                document.getText() + "\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

            messageService.sendMessage("EMAIL", reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", emailBody);

            ctx.json(reservation);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));

            Reservation reservation = reservationService.getReservation(id);
            if (reservation == null) {
                throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
            }

            reservationService.cancelReservation(id);
            reservation = reservationService.getReservation(id);

            ctx.json(reservation);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));

            Reservation reservation = reservationService.getReservation(id);
            if (reservation == null) {
                throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
            }

            reservationService.checkInReservation(id);
            reservation = reservationService.getReservation(id);

            ctx.json(reservation);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));

            Reservation reservation = reservationService.getReservation(id);
            if (reservation == null) {
                throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
            }

            reservationService.checkOutReservation(id);
            reservation = reservationService.getReservation(id);

            Document document = documentService.generateDocument(reservation);

            ctx.json(document);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));

            Reservation reservation = reservationService.getReservation(id);
            if (reservation == null) {
                throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
            }

            if (!reservation.getStatus().equals("CONFIRMED") &&
                !reservation.getStatus().equals("CHECKED_IN") &&
                !reservation.getStatus().equals("CHECKED_OUT")) {
                throw new ApiException("OPERATION_NOT_ALLOWED", "Document only available for paid reservations", 409);
            }

            Document document = documentService.generateDocument(reservation);

            ctx.json(document);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e.getCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            sendErrorResponse(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    private void sendErrorResponse(Context ctx, String code, String message, int statusCode) {
        ctx.status(statusCode);
        ctx.json(new ErrorResponse(code, message));
    }

    private String getStringField(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }

    private int getIntField(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asInt();
        }
        return 0;
    }

    private LocalDate getDateField(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            String dateStr = node.get(fieldName).asText();
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
