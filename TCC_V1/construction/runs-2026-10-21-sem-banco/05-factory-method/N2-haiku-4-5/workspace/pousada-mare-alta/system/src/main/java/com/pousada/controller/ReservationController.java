package com.pousada.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.dto.CreateReservationRequest;
import com.pousada.dto.DocumentResponse;
import com.pousada.dto.ErrorResponse;
import com.pousada.dto.ReservationResponse;
import com.pousada.service.DocumentService;
import com.pousada.service.MessageService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final DocumentService documentService;
    private final Gson gson = new Gson();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationController(ReservationService reservationService, MessageService messageService,
            DocumentService documentService) {
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.documentService = documentService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkInReservation);
        app.post("/reservations/{id}/check-out", this::checkOutReservation);
        app.get("/reservations/{id}/document", this::getDocument);
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null || req.checkIn == null
                    || req.checkOut == null || req.guests == 0 || req.ratePlan == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int identificationCount = (req.guestCpf != null ? 1 : 0) + (req.companyCnpj != null ? 1 : 0)
                    + (req.guestPassport != null ? 1 : 0);
            if (identificationCount != 1) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Exactly one of guestCpf, companyCnpj, or guestPassport must be provided"));
                return;
            }

            if (req.companyCnpj != null && req.companyName == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "companyName is required when companyCnpj is provided"));
                return;
            }

            if (req.guestPassport != null && req.guestCountry == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "guestCountry is required when guestPassport is provided"));
                return;
            }

            LocalDate checkIn, checkOut;
            try {
                checkIn = LocalDate.parse(req.checkIn, DATE_FORMAT);
                checkOut = LocalDate.parse(req.checkOut, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format, use yyyy-MM-dd"));
                return;
            }

            if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(req.ratePlan);
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            try {
                Reservation reservation = reservationService.createReservation(req.roomNumber, req.guestName,
                        req.guestEmail, req.guestCpf, req.companyCnpj, req.companyName, req.guestPassport,
                        req.guestCountry, checkIn, checkOut, req.guests, ratePlan);
                ctx.status(201);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                String msg = e.getMessage();
                if (msg.equals("Room not found")) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("ROOM_NOT_FOUND", msg));
                } else if (msg.contains("unavailable")) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", msg));
                } else if (msg.equals("Invalid number of guests")) {
                    ctx.status(422);
                    ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", msg));
                } else {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", msg));
                }
            }
        } catch (JsonSyntaxException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON"));
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Can only confirm a pending reservation"));
                return;
            }

            reservation = reservationService.confirmReservation(id, reservation.getTotal());

            var document = documentService.generateDocument(reservation);
            var emailBody = formatConfirmationEmail(reservation, document.text());

            messageService.sendMessage(MessageChannel.EMAIL, reservation.getGuestEmail(),
                    "Reserva confirmada - Pousada Maré Alta", emailBody);

            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Can only cancel a pending or confirmed reservation"));
                return;
            }

            reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void checkInReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Can only check in a confirmed reservation"));
                return;
            }

            reservation = reservationService.checkInReservation(id);
            ctx.json(ReservationResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void checkOutReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Can only check out a checked-in reservation"));
                return;
            }

            reservation = reservationService.checkOutReservation(id);
            var document = documentService.generateDocument(reservation);
            ctx.json(DocumentResponse.fromDocumentContent(document));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void getDocument(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CONFIRMED && reservation.getStatus() != ReservationStatus.CHECKED_IN
                    && reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document is only available for paid reservations"));
                return;
            }

            var document = documentService.generateDocument(reservation);
            ctx.json(DocumentResponse.fromDocumentContent(document));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private String formatConfirmationEmail(Reservation reservation, String documentText) {
        return String.format(
                "Olá, %s!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + "%s\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!",
                reservation.getGuestName(), documentText);
    }
}
