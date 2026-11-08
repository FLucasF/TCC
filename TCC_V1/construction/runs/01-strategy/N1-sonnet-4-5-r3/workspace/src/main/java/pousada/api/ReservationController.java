package pousada.api;

import io.javalin.http.Context;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.repository.ReservationRepository;
import pousada.repository.RoomRepository;
import pousada.service.NotificationService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReservationController(ReservationRepository reservationRepository,
                                RoomRepository roomRepository,
                                NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = getRequiredString(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = parseDate(getRequiredString(body, "checkIn"));
            LocalDate checkOut = parseDate(getRequiredString(body, "checkOut"));
            int guests = getRequiredInt(body, "guests");
            String ratePlan = getRequiredString(body, "ratePlan");
            String companyCode = (String) body.get("companyCode");

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (guests < 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Number of guests must be at least 1"));
                return;
            }

            Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

            if (guests > room.capacity()) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                return;
            }

            List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
            if (!conflicts.isEmpty()) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
                return;
            }

            Reservation reservation = Reservation.create(
                roomNumber, room.type(), guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode
            );

            reservationRepository.save(reservation);

            ctx.status(201).json(toJson(reservation));

        } catch (InvalidRequestException | DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

            ctx.json(toJson(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

            reservation.confirm();
            reservationRepository.save(reservation);
            notificationService.sendConfirmationEmail(reservation);

            ctx.json(toJson(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

            reservation.cancel();
            reservationRepository.save(reservation);

            ctx.json(toJson(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

            reservation.doCheckIn();
            reservationRepository.save(reservation);

            ctx.json(toJson(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

            reservation.doCheckOut();
            reservationRepository.save(reservation);

            ctx.json(toDocument(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            ctx.json(toDocument(reservation));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    private Map<String, Object> toJson(Reservation reservation) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", reservation.id());
        json.put("roomNumber", reservation.roomNumber());
        json.put("guestName", reservation.guestName());
        json.put("guestEmail", reservation.guestEmail());
        json.put("guestCpf", reservation.guestCpf());
        json.put("checkIn", reservation.checkIn().toString());
        json.put("checkOut", reservation.checkOut().toString());
        json.put("guests", reservation.guests());
        json.put("ratePlan", reservation.ratePlanName());
        json.put("nights", reservation.nights());
        json.put("subtotal", reservation.subtotal());
        json.put("discount", reservation.discount());
        json.put("total", reservation.total());
        json.put("status", reservation.status().name());
        json.put("amountPaid", reservation.amountPaid());
        json.put("refund", reservation.refund());
        return json;
    }

    private Map<String, Object> toDocument(Reservation reservation) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("title", "Comprovante de reserva");
        doc.put("reservationId", reservation.id());
        doc.put("guestName", reservation.guestName());
        doc.put("guestCpf", reservation.guestCpf());
        doc.put("roomNumber", reservation.roomNumber());
        doc.put("roomType", reservation.roomType().name());
        doc.put("checkIn", reservation.checkIn().toString());
        doc.put("checkOut", reservation.checkOut().toString());
        doc.put("nights", reservation.nights());
        doc.put("subtotal", reservation.subtotal());
        doc.put("discount", reservation.discount());
        doc.put("total", reservation.total());
        return doc;
    }

    private String getRequiredString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || !(value instanceof String) || ((String) value).isEmpty()) {
            throw new InvalidRequestException("Missing or invalid field: " + key);
        }
        return (String) value;
    }

    private int getRequiredInt(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            throw new InvalidRequestException("Missing field: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new InvalidRequestException("Invalid field type: " + key);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + dateStr);
        }
    }

    private static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    private static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }

    private static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }
}
