package com.pousadamarealta.web;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.messaging.Message;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

public class HotelController {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    public HotelController(RoomRepository roomRepository,
                           ReservationRepository reservationRepository,
                           ReservationService reservationService,
                           MessageService messageService,
                           ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
                .map(RoomResponse::new)
                .toList();
        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            // Validações
            if (request.getGuestName() == null || request.getGuestEmail() == null ||
                    request.getGuestCpf() == null || request.getCheckIn() == null ||
                    request.getCheckOut() == null || request.getRatePlan() == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            if (!DATE_PATTERN.matcher(request.getCheckIn()).matches() ||
                    !DATE_PATTERN.matcher(request.getCheckOut()).matches()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(request.getCheckIn());
                checkOut = LocalDate.parse(request.getCheckOut());
            } catch (DateTimeParseException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (request.getGuests() < 1) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Guests must be at least 1"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.getRatePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Room room = roomRepository.findByNumber(request.getRoomNumber())
                    .orElse(null);
            if (room == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            if (request.getGuests() > room.getCapacity()) {
                ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                        .json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                return;
            }

            if (!reservationRepository.isRoomAvailable(request.getRoomNumber(), checkIn, checkOut, null)) {
                ctx.status(HttpStatus.CONFLICT)
                        .json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                    request.getRoomNumber(),
                    request.getGuestName(),
                    request.getGuestEmail(),
                    request.getGuestCpf(),
                    checkIn,
                    checkOut,
                    request.getGuests(),
                    ratePlan
            );

            ctx.status(HttpStatus.CREATED)
                    .json(new ReservationResponse(reservation, room));

        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(new ReservationResponse(reservation, room));
    }

    public void confirmReservation(Context ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        try {
            reservation = reservationService.confirmReservation(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(new ReservationResponse(reservation, room));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
                    .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        try {
            reservation = reservationService.cancel(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(new ReservationResponse(reservation, room));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
                    .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        try {
            reservation = reservationService.checkIn(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(new ReservationResponse(reservation, room));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
                    .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        try {
            reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

            DocumentResponse document = createDocumentResponse(reservation, room);
            ctx.json(document);
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
                    .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
                reservation.getStatus() != ReservationStatus.CHECKED_IN &&
                reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            ctx.status(HttpStatus.CONFLICT)
                    .json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
            return;
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        DocumentResponse document = createDocumentResponse(reservation, room);
        ctx.json(document);
    }

    private DocumentResponse createDocumentResponse(Reservation reservation, Room room) {
        DocumentResponse doc = new DocumentResponse();
        doc.setTitle("Comprovante de reserva");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(formatCpf(reservation.getGuestCpf()));
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(room.getType().name());
        doc.setCheckIn(reservation.getCheckIn().toString());
        doc.setCheckOut(reservation.getCheckOut().toString());
        doc.setNights(reservation.getNights());

        double subtotal = reservation.calculateSubtotal(room.getNightlyRate()).doubleValue();
        double discount = reservation.calculateDiscount(reservation.calculateSubtotal(room.getNightlyRate())).doubleValue();
        double total = reservation.calculateTotal(room.getNightlyRate()).doubleValue();

        doc.setSubtotal(subtotal);
        doc.setDiscount(discount > 0 ? discount : null);
        doc.setTotal(total);

        return doc;
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getSentMessages().stream()
                .map(MessageResponse::new)
                .toList();
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || !DATE_PATTERN.matcher(dateParam).matches()) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8")
                    .result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || !MONTH_PATTERN.matcher(monthParam).matches()) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            return;
        }

        try {
            String[] parts = monthParam.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("INVALID_REQUEST", "Invalid month"));
                return;
            }

            String csv = reportService.generateRevenueReport(year, month);
            ctx.contentType("text/csv; charset=utf-8")
                    .result(csv);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
