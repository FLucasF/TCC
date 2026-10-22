package br.pousada.marealta.api;

import io.javalin.Javalin;
import br.pousada.marealta.model.*;
import br.pousada.marealta.database.Database;
import br.pousada.marealta.database.RoomRepository;
import br.pousada.marealta.database.ReservationRepository;
import br.pousada.marealta.service.PricingService;
import br.pousada.marealta.service.MessageService;
import br.pousada.marealta.service.DocumentFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReservationHandler.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> createReservation(ctx));
        app.get("/reservations/{id}", ctx -> getReservation(ctx));
        app.post("/reservations/{id}/confirmation", ctx -> confirmReservation(ctx));
        app.post("/reservations/{id}/cancellation", ctx -> cancelReservation(ctx));
        app.post("/reservations/{id}/check-in", ctx -> checkIn(ctx));
        app.post("/reservations/{id}/check-out", ctx -> checkOut(ctx));
        app.get("/reservations/{id}/document", ctx -> getDocument(ctx));
    }

    private static void createReservation(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            if (req == null || !hasRequiredFields(req)) {
                ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = req.roomNumber;
            String guestName = req.guestName;
            String guestEmail = req.guestEmail;
            String guestCpf = req.guestCpf;
            LocalDate checkIn = LocalDate.parse(req.checkIn, DATE_FORMAT);
            LocalDate checkOut = LocalDate.parse(req.checkOut, DATE_FORMAT);
            int guests = req.guests;
            RatePlan ratePlan = RatePlan.valueOf(req.ratePlan);

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (guests < 1) {
                ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "At least one guest is required"));
                return;
            }

            var roomOpt = RoomRepository.findByNumber(roomNumber);
            if (roomOpt.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            Room room = roomOpt.get();
            if (guests > room.getCapacity()) {
                ctx.status(422).json(ErrorResponse.error("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                return;
            }

            if (!ReservationRepository.isRoomAvailable(conn, roomNumber, checkIn, checkOut)) {
                ctx.status(409).json(ErrorResponse.error("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
                return;
            }

            int nights = PricingService.calculateNights(checkIn, checkOut);
            double subtotal = 0;
            for (int i = 0; i < nights; i++) {
                LocalDate date = checkIn.plusDays(i);
                double nightlyRate = PricingService.calculateNightlyRateForDate(room.getNightlyRate(), date);
                subtotal += nightlyRate;
            }
            subtotal = PricingService.round(subtotal);

            double discount = PricingService.calculateDiscount(subtotal, ratePlan.toString());
            double total = PricingService.calculateTotal(subtotal, discount);

            Reservation reservation = new Reservation(
                0,
                roomNumber,
                guestName,
                guestEmail,
                guestCpf,
                checkIn,
                checkOut,
                guests,
                ratePlan,
                nights,
                subtotal,
                discount,
                total,
                ReservationStatus.PENDING,
                0,
                null
            );

            Reservation saved = ReservationRepository.insert(conn, reservation);
            ctx.status(201).json(saved);

        } catch (IllegalArgumentException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid request format"));
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to create reservation"));
        }
    }

    private static void getReservation(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = ReservationRepository.findById(conn, id);

            if (reservation.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(reservation.get());
        } catch (NumberFormatException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error getting reservation", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to get reservation"));
        }
    }

    private static void confirmReservation(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservationOpt = ReservationRepository.findById(conn, id);

            if (reservationOpt.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation reservation = reservationOpt.get();
            if (reservation.getStatus() != ReservationStatus.PENDING) {
                ctx.status(409).json(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Can only confirm pending reservations"));
                return;
            }

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setAmountPaid(reservation.getTotal());
            ReservationRepository.update(conn, reservation);

            var roomOpt = RoomRepository.findByNumber(reservation.getRoomNumber());
            Room room = roomOpt.get();

            String documentText = DocumentFormatter.format(new Document(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount() > 0 ? reservation.getDiscount() : null,
                reservation.getTotal()
            ));

            MessageService.sendConfirmationEmail(conn, reservation.getId(), reservation.getGuestName(), reservation.getGuestEmail(), documentText);

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to confirm reservation"));
        }
    }

    private static void cancelReservation(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservationOpt = ReservationRepository.findById(conn, id);

            if (reservationOpt.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation reservation = reservationOpt.get();
            if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409).json(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Can only cancel pending or confirmed reservations"));
                return;
            }

            double refund = 0;
            if (reservation.getStatus() == ReservationStatus.CONFIRMED && reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }

            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setRefund(refund);
            ReservationRepository.update(conn, reservation);

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error cancelling reservation", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to cancel reservation"));
        }
    }

    private static void checkIn(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservationOpt = ReservationRepository.findById(conn, id);

            if (reservationOpt.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation reservation = reservationOpt.get();
            if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409).json(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Can only check in confirmed reservations"));
                return;
            }

            reservation.setStatus(ReservationStatus.CHECKED_IN);
            ReservationRepository.update(conn, reservation);

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error checking in reservation", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to check in"));
        }
    }

    private static void checkOut(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservationOpt = ReservationRepository.findById(conn, id);

            if (reservationOpt.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation reservation = reservationOpt.get();
            if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                ctx.status(409).json(ErrorResponse.error("OPERATION_NOT_ALLOWED", "Can only check out reservations that are checked in"));
                return;
            }

            reservation.setStatus(ReservationStatus.CHECKED_OUT);
            ReservationRepository.update(conn, reservation);

            var roomOpt = RoomRepository.findByNumber(reservation.getRoomNumber());
            Room room = roomOpt.get();

            Document doc = new Document(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount() > 0 ? reservation.getDiscount() : null,
                reservation.getTotal()
            );

            ctx.json(doc);
        } catch (NumberFormatException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error checking out reservation", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to check out"));
        }
    }

    private static void getDocument(io.javalin.http.Context ctx) {
        try (Connection conn = Database.getConnection()) {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservationOpt = ReservationRepository.findById(conn, id);

            if (reservationOpt.isEmpty()) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation reservation = reservationOpt.get();
            if (reservation.getStatus() == ReservationStatus.PENDING || reservation.getStatus() == ReservationStatus.CANCELLED) {
                ctx.status(404).json(ErrorResponse.error("RESERVATION_NOT_FOUND", "Document only available for confirmed reservations"));
                return;
            }

            var roomOpt = RoomRepository.findByNumber(reservation.getRoomNumber());
            Room room = roomOpt.get();

            Document doc = new Document(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount() > 0 ? reservation.getDiscount() : null,
                reservation.getTotal()
            );

            ctx.json(doc);
        } catch (NumberFormatException e) {
            ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error getting document", e);
            ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to get document"));
        }
    }

    private static boolean hasRequiredFields(CreateReservationRequest req) {
        return req.roomNumber > 0 && req.guestName != null && req.guestEmail != null &&
               req.guestCpf != null && req.checkIn != null && req.checkOut != null &&
               req.guests > 0 && req.ratePlan != null;
    }
}
