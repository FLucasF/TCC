package com.pousadamarealta;

import io.javalin.Javalin;
import java.time.LocalDate;
import java.util.Map;

public class ReservationController {
    private final Database database;
    private final ReservationService service;

    public ReservationController(Database database) {
        this.database = database;
        this.service = new ReservationService(database);
    }

    public void register(Javalin app) {
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.statusCode);
            ctx.json(Map.of(
                "error", e.errorCode,
                "message", e.getMessage()
            ));
        });
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            var body = ctx.bodyAsClass(ReservationRequest.class);

            validateIdentification(body);
            validateDates(body.checkIn, body.checkOut);
            validateGuests(body.guests);

            LocalDate checkIn = LocalDate.parse(body.checkIn);
            LocalDate checkOut = LocalDate.parse(body.checkOut);
            RatePlan ratePlan = RatePlan.fromString(body.ratePlan);

            Reservation reservation = service.createReservation(
                body.roomNumber,
                body.guestName,
                body.guestEmail,
                body.guestCpf,
                body.companyCnpj,
                body.companyName,
                body.guestPassport,
                body.guestCountry,
                checkIn,
                checkOut,
                body.guests,
                ratePlan
            );

            ctx.status(201);
            ctx.json(reservation);
        } catch (Exception e) {
            if (e instanceof ApiException ae) {
                throw ae;
            }
            throw new ApiException("INVALID_REQUEST", "Invalid request body", 400);
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = service.getReservationById(id);
            ctx.json(reservation);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var body = ctx.bodyAsClass(ConfirmationRequest.class);

            Reservation reservation = service.confirmReservation(id, body.amountPaid);
            ctx.json(reservation);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("INVALID_REQUEST", "Invalid request", 400);
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = service.cancelReservation(id);
            ctx.json(reservation);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
    }

    private void checkIn(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = service.checkIn(id);
            ctx.json(reservation);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
    }

    private void checkOut(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = service.checkOut(id);
            RoomType roomType = getRoomType(reservation.roomNumber);
            Document document = DocumentService.generateDocument(reservation, roomType);
            ctx.json(document);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
    }

    private void getDocument(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = service.getReservationById(id);

            if (reservation.status == ReservationStatus.PENDING ||
                (reservation.status == ReservationStatus.CANCELLED && reservation.amountPaid == 0)) {
                throw new ApiException("OPERATION_NOT_ALLOWED", "Document not available for this reservation", 409);
            }

            RoomType roomType = getRoomType(reservation.roomNumber);
            Document document = DocumentService.generateDocument(reservation, roomType);
            ctx.json(document);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
    }

    private void validateIdentification(ReservationRequest body) {
        int identificationCount = 0;
        if (body.guestCpf != null) identificationCount++;
        if (body.companyCnpj != null) identificationCount++;
        if (body.guestPassport != null) identificationCount++;

        if (identificationCount != 1) {
            throw new ApiException("INVALID_REQUEST", "Exactly one identification method required", 400);
        }

        if (body.companyCnpj != null && body.companyName == null) {
            throw new ApiException("INVALID_REQUEST", "Company name required with CNPJ", 400);
        }

        if (body.guestPassport != null && body.guestCountry == null) {
            throw new ApiException("INVALID_REQUEST", "Country required with passport", 400);
        }
    }

    private void validateDates(String checkIn, String checkOut) {
        try {
            LocalDate in = LocalDate.parse(checkIn);
            LocalDate out = LocalDate.parse(checkOut);
            if (!out.isAfter(in)) {
                throw new ApiException("INVALID_REQUEST", "Invalid date range", 400);
            }
        } catch (Exception e) {
            throw new ApiException("INVALID_REQUEST", "Invalid date format", 400);
        }
    }

    private void validateGuests(int guests) {
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Must have at least 1 guest", 400);
        }
    }

    private RoomType getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> RoomType.STANDARD;
            case 201, 202 -> RoomType.DELUXE;
            case 301 -> RoomType.SUITE;
            default -> null;
        };
    }

    static class ReservationRequest {
        public int roomNumber;
        public String guestName;
        public String guestEmail;
        public String guestCpf;
        public String companyCnpj;
        public String companyName;
        public String guestPassport;
        public String guestCountry;
        public String checkIn;
        public String checkOut;
        public int guests;
        public String ratePlan;
    }

    static class ConfirmationRequest {
        public double amountPaid;
    }
}
