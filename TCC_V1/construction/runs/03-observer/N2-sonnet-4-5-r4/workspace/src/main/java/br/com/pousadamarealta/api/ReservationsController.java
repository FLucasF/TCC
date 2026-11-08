package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.CreateReservationRequest;
import br.com.pousadamarealta.api.dto.DocumentDTO;
import br.com.pousadamarealta.api.dto.ErrorResponse;
import br.com.pousadamarealta.api.dto.ReservationDTO;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.Room;
import br.com.pousadamarealta.repository.RoomRepository;
import br.com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationsController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationsController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

                if (request.roomNumber() == null || request.guestName() == null ||
                    request.guestEmail() == null || request.guestCpf() == null ||
                    request.checkIn() == null || request.checkOut() == null ||
                    request.guests() == null || request.ratePlan() == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                LocalDate checkIn = LocalDate.parse(request.checkIn());
                LocalDate checkOut = LocalDate.parse(request.checkOut());

                if (!checkOut.isAfter(checkIn)) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                    return;
                }

                Reservation reservation = reservationService.create(
                    request.roomNumber(),
                    request.guestName(),
                    request.guestEmail(),
                    request.guestCpf(),
                    checkIn,
                    checkOut,
                    request.guests(),
                    request.ratePlan()
                );

                Room room = roomRepository.findByNumber(reservation.getRoomNumber());
                ctx.status(HttpStatus.CREATED);
                ctx.json(ReservationDTO.from(reservation, room));

            } catch (DateTimeParseException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.findById(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            ctx.json(ReservationDTO.from(reservation, room));
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.confirm(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            ctx.json(ReservationDTO.from(reservation, room));
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.cancel(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            ctx.json(ReservationDTO.from(reservation, room));
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkIn(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            ctx.json(ReservationDTO.from(reservation, room));
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkOut(id);
            String document = reservationService.getDocument(id);

            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
            BigDecimal discount = reservation.calculateDiscount(subtotal);
            BigDecimal total = subtotal.subtract(discount);

            ctx.json(DocumentDTO.from(reservation, room.getType(), subtotal, discount, total));
        });

        app.get("/reservations/{id}/document", ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.findById(id);

            if (!reservation.isPaid()) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document is only available for paid reservations"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
            BigDecimal discount = reservation.calculateDiscount(subtotal);
            BigDecimal total = subtotal.subtract(discount);

            ctx.json(DocumentDTO.from(reservation, room.getType(), subtotal, discount, total));
        });
    }
}
