package br.com.marealta.controller;

import br.com.marealta.dto.CreateReservationRequest;
import br.com.marealta.dto.DocumentDTO;
import br.com.marealta.dto.ReservationDTO;
import br.com.marealta.exception.InvalidRequestException;
import br.com.marealta.model.GuestIdentification;
import br.com.marealta.model.RatePlan;
import br.com.marealta.model.Reservation;
import br.com.marealta.model.Room;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.DocumentService;
import br.com.marealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final DocumentService documentService;

    public ReservationController(
            ReservationService reservationService,
            RoomRepository roomRepository,
            DocumentService documentService) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.documentService = documentService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        GuestIdentification guestId = parseGuestIdentification(request);
        LocalDate checkIn = parseDate(request.checkIn(), "checkIn");
        LocalDate checkOut = parseDate(request.checkOut(), "checkOut");
        RatePlan ratePlan = parseRatePlan(request.ratePlan());

        if (request.guests() == null) {
            throw new InvalidRequestException("Missing required field: guests");
        }

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            guestId,
            checkIn,
            checkOut,
            request.guests(),
            ratePlan
        );

        ctx.status(201).json(ReservationDTO.from(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = parseLongParam(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = parseLongParam(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = parseLongParam(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = parseLongParam(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationDTO.from(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = parseLongParam(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String documentText = documentService.generateDocument(reservation, room);
        ctx.json(DocumentDTO.from(reservation, room, documentText));
    }

    public void getDocument(Context ctx) {
        Long id = parseLongParam(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().doubleValue() == 0) {
            throw new InvalidRequestException("Document only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String documentText = documentService.generateDocument(reservation, room);
        ctx.json(DocumentDTO.from(reservation, room, documentText));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new InvalidRequestException("Missing required field: roomNumber");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new InvalidRequestException("Missing required field: guestName");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new InvalidRequestException("Missing required field: guestEmail");
        }
        if (request.checkIn() == null || request.checkIn().isBlank()) {
            throw new InvalidRequestException("Missing required field: checkIn");
        }
        if (request.checkOut() == null || request.checkOut().isBlank()) {
            throw new InvalidRequestException("Missing required field: checkOut");
        }
        if (request.ratePlan() == null || request.ratePlan().isBlank()) {
            throw new InvalidRequestException("Missing required field: ratePlan");
        }
    }

    private GuestIdentification parseGuestIdentification(CreateReservationRequest request) {
        boolean hasCpf = request.guestCpf() != null && !request.guestCpf().isBlank();
        boolean hasCnpj = request.companyCnpj() != null && !request.companyCnpj().isBlank();
        boolean hasPassport = request.guestPassport() != null && !request.guestPassport().isBlank();

        int identificationCount = (hasCpf ? 1 : 0) + (hasCnpj ? 1 : 0) + (hasPassport ? 1 : 0);

        if (identificationCount != 1) {
            throw new InvalidRequestException(
                "Must provide exactly one identification type: guestCpf, or companyCnpj+companyName, or guestPassport+guestCountry"
            );
        }

        if (hasCpf) {
            return new GuestIdentification.IndividualGuest(request.guestCpf());
        }

        if (hasCnpj) {
            if (request.companyName() == null || request.companyName().isBlank()) {
                throw new InvalidRequestException("companyName is required when companyCnpj is provided");
            }
            return new GuestIdentification.CorporateGuest(request.companyCnpj(), request.companyName());
        }

        if (hasPassport) {
            if (request.guestCountry() == null || request.guestCountry().isBlank()) {
                throw new InvalidRequestException("guestCountry is required when guestPassport is provided");
            }
            return new GuestIdentification.ForeignGuest(request.guestPassport(), request.guestCountry());
        }

        throw new InvalidRequestException("No valid guest identification provided");
    }

    private LocalDate parseDate(String date, String fieldName) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format for " + fieldName + ": " + date);
        }
    }

    private RatePlan parseRatePlan(String ratePlan) {
        try {
            return RatePlan.valueOf(ratePlan);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan: " + ratePlan);
        }
    }

    private Long parseLongParam(String param) {
        try {
            return Long.parseLong(param);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid ID: " + param);
        }
    }
}
