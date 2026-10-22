package com.pousada.marealta.handlers;

import com.pousada.marealta.dto.CreateReservationRequest;
import com.pousada.marealta.dto.DocumentResponse;
import com.pousada.marealta.dto.ReservationResponse;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.util.DocumentFormatter;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReservationHandler.class);

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                var req = ctx.bodyAsClass(CreateReservationRequest.class);
                var res = ReservationService.createReservation(req);
                ctx.status(201);
                ctx.json(toResponse(res));
            } catch (ReservationService.ValidationError e) {
                int status = switch (e.code) {
                    case "ROOM_NOT_FOUND" -> 404;
                    case "CAPACITY_EXCEEDED" -> 422;
                    case "ROOM_UNAVAILABLE" -> 409;
                    default -> 400;
                };
                ctx.status(status);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error creating reservation", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = ctx.pathParamAsClass("id", Long.class).get();
                var res = ReservationRepository.findById(id)
                    .orElseThrow(() -> new ReservationService.ValidationError("RESERVATION_NOT_FOUND", "Reservation not found"));
                ctx.json(toResponse(res));
            } catch (ReservationService.ValidationError e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error fetching reservation", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = ctx.pathParamAsClass("id", Long.class).get();

                // Parse amount paid from body
                var body = ctx.body();
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var jsonNode = mapper.readTree(body);
                var amountPaid = new java.math.BigDecimal(jsonNode.get("amountPaid").asText());

                var res = ReservationService.confirm(id, amountPaid);

                // Send confirmation email
                try {
                    MessageService.sendConfirmationEmail(res);
                } catch (Exception e) {
                    logger.error("Error sending confirmation email", e);
                }

                ctx.json(toResponse(res));
            } catch (ReservationService.ValidationError e) {
                int status = e.code.equals("RESERVATION_NOT_FOUND") ? 404 : 409;
                ctx.status(status);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error confirming reservation", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = ctx.pathParamAsClass("id", Long.class).get();
                var res = ReservationService.checkIn(id);
                ctx.json(toResponse(res));
            } catch (ReservationService.ValidationError e) {
                int status = e.code.equals("RESERVATION_NOT_FOUND") ? 404 : 409;
                ctx.status(status);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error checking in reservation", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = ctx.pathParamAsClass("id", Long.class).get();
                var res = ReservationService.checkOut(id);
                var room = RoomRepository.findById(res.getRoomNumber()).orElseThrow();
                ctx.json(getDocumentResponse(res, room.type()));
            } catch (ReservationService.ValidationError e) {
                int status = e.code.equals("RESERVATION_NOT_FOUND") ? 404 : 409;
                ctx.status(status);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error checking out reservation", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = ctx.pathParamAsClass("id", Long.class).get();
                var res = ReservationService.cancel(id);
                ctx.json(toResponse(res));
            } catch (ReservationService.ValidationError e) {
                int status = e.code.equals("RESERVATION_NOT_FOUND") ? 404 : 409;
                ctx.status(status);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error cancelling reservation", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = ctx.pathParamAsClass("id", Long.class).get();
                var res = ReservationRepository.findById(id)
                    .orElseThrow(() -> new ReservationService.ValidationError("RESERVATION_NOT_FOUND", "Reservation not found"));

                if (res.getAmountPaid().signum() == 0) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                    return;
                }

                var room = RoomRepository.findById(res.getRoomNumber()).orElseThrow();
                ctx.json(getDocumentResponse(res, room.type()));
            } catch (ReservationService.ValidationError e) {
                int status = e.code.equals("RESERVATION_NOT_FOUND") ? 404 : 409;
                ctx.status(status);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (Exception e) {
                logger.error("Error fetching document", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }

    private static ReservationResponse toResponse(Reservation res) {
        var resp = new ReservationResponse();
        resp.id = res.getId();
        resp.roomNumber = res.getRoomNumber();
        resp.guestName = res.getGuestName();
        resp.guestEmail = res.getGuestEmail();
        resp.guestCpf = res.getGuestCpf();
        resp.companyCnpj = res.getCompanyCnpj();
        resp.companyName = res.getCompanyName();
        resp.guestPassport = res.getGuestPassport();
        resp.guestCountry = res.getGuestCountry();
        resp.checkIn = DocumentFormatter.formatDateISO(res.getCheckIn());
        resp.checkOut = DocumentFormatter.formatDateISO(res.getCheckOut());
        resp.guests = res.getGuests();
        resp.ratePlan = res.getRatePlan().toString();
        resp.nights = res.getNights();
        resp.subtotal = res.getSubtotal();
        resp.discount = res.getDiscount();
        resp.total = res.getTotal();
        resp.status = res.getStatus().toString();
        resp.amountPaid = res.getAmountPaid();
        resp.refund = res.getRefund();
        return resp;
    }

    private static DocumentResponse getDocumentResponse(Reservation res, String roomType) {
        var doc = new DocumentResponse();
        doc.reservationId = res.getId();
        doc.guestName = res.getGuestName();
        doc.guestCpf = res.getGuestCpf();
        doc.companyCnpj = res.getCompanyCnpj();
        doc.companyName = res.getCompanyName();
        doc.guestPassport = res.getGuestPassport();
        doc.guestCountry = res.getGuestCountry();
        doc.roomNumber = res.getRoomNumber();
        doc.roomType = roomType;
        doc.checkIn = DocumentFormatter.formatDateISO(res.getCheckIn());
        doc.checkOut = DocumentFormatter.formatDateISO(res.getCheckOut());
        doc.nights = res.getNights();
        doc.subtotal = res.getSubtotal();
        doc.discount = res.getDiscount();
        doc.total = res.getTotal();

        if (res.getGuestCpf() != null) {
            doc.title = "Comprovante de reserva";
            doc.text = DocumentFormatter.formatComprovante(res, roomType);
        } else if (res.getCompanyCnpj() != null) {
            doc.title = "Fatura de hospedagem";
            doc.text = DocumentFormatter.formatFatura(res, roomType);
            doc.iss = res.getTotal().multiply(new java.math.BigDecimal("0.05"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        } else if (res.getGuestPassport() != null) {
            doc.title = "Booking receipt";
            doc.text = DocumentFormatter.formatBookingReceipt(res, roomType);
        }

        return doc;
    }

    record ErrorResponse(String error, String message) {}
}
