package br.com.pousada.marealta.api;

import br.com.pousada.marealta.api.dto.*;
import br.com.pousada.marealta.model.*;
import br.com.pousada.marealta.service.ReservationService;
import io.javalin.Javalin;

public class ReservationController {
    private static final String RESERVATION_NOT_FOUND_MSG = "Reserva não encontrada";

    public static void register(Javalin app, ReservationService reservationService) {
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

                if (req.roomNumber() == 0 || req.guestName() == null || req.guestEmail() == null ||
                    req.guestCpf() == null || req.checkIn() == null || req.checkOut() == null ||
                    req.ratePlan() == null) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }

                RatePlan ratePlan = RatePlan.valueOf(req.ratePlan());
                Reservation reservation = reservationService.createReservation(
                        req.roomNumber(),
                        req.guestName(),
                        req.guestEmail(),
                        req.guestCpf(),
                        req.checkIn(),
                        req.checkOut(),
                        req.guests(),
                        ratePlan
                );

                ctx.status(201);
                ctx.json(ReservationResponse.from(reservation));
            } catch (IllegalArgumentException e) {
                if ("INVALID_REQUEST".equals(e.getMessage())) {
                    throw e;
                }
                if ("ROOM_NOT_FOUND".equals(e.getMessage())) {
                    throw e;
                }
                if ("ROOM_UNAVAILABLE".equals(e.getMessage())) {
                    throw e;
                }
                if ("CAPACITY_EXCEEDED".equals(e.getMessage())) {
                    throw e;
                }
                throw new IllegalArgumentException("INVALID_REQUEST");
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        });

        app.get("/reservations/{id}", ctx -> {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            ConfirmationRequest req = ctx.bodyAsClass(ConfirmationRequest.class);
            Reservation reservation = reservationService.confirm(id, req.amountPaid);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkOut(id);
            Room room = reservationService.getRoom(reservation.getRoomNumber());
            ReservationDocument document = ReservationDocument.from(reservation, room.type());
            ctx.json(ReservationDocumentResponse.from(document));
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.cancel(id);
            ctx.json(ReservationResponse.from(reservation));
        });

        app.get("/reservations/{id}/document", ctx -> {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
                reservation.getStatus() != ReservationStatus.CHECKED_IN &&
                reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", RESERVATION_NOT_FOUND_MSG));
                return;
            }

            Room room = reservationService.getRoom(reservation.getRoomNumber());
            ReservationDocument document = ReservationDocument.from(reservation, room.type());
            ctx.json(ReservationDocumentResponse.from(document));
        });
    }

    record ConfirmationRequest(double amountPaid) {}
}
