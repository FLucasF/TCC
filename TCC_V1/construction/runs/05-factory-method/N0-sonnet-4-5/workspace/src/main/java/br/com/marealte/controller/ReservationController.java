package br.com.marealte.controller;

import br.com.marealte.dto.CreateReservationRequest;
import br.com.marealte.dto.ErrorResponse;
import br.com.marealte.model.*;
import br.com.marealte.repository.MessageRepository;
import br.com.marealte.repository.ReservationRepository;
import br.com.marealte.repository.RoomRepository;
import br.com.marealte.service.DocumentService;
import br.com.marealte.service.EmailService;
import br.com.marealte.service.PricingService;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final EmailService emailService;

    public ReservationController(ReservationRepository reservationRepository,
                                  RoomRepository roomRepository,
                                  MessageRepository messageRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.emailService = new EmailService(messageRepository);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (!validateRequest(request, ctx)) {
                return;
            }

            if (!validateIdentification(request, ctx)) {
                return;
            }

            Room room = roomRepository.findByNumber(request.getRoomNumber());
            if (room == null) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            if (request.getCheckOut() == null || !request.getCheckOut().isAfter(request.getCheckIn())) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (request.getGuests() < 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "At least one guest is required"));
                return;
            }

            if (request.getGuests() > room.getCapacity()) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Room capacity exceeded"));
                return;
            }

            if (!reservationRepository.isRoomAvailable(request.getRoomNumber(), request.getCheckIn(), request.getCheckOut(), null)) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
                return;
            }

            Reservation reservation = new Reservation();
            reservation.setRoomNumber(request.getRoomNumber());
            reservation.setGuestName(request.getGuestName());
            reservation.setGuestEmail(request.getGuestEmail());
            reservation.setGuestCpf(request.getGuestCpf());
            reservation.setCompanyCnpj(request.getCompanyCnpj());
            reservation.setCompanyName(request.getCompanyName());
            reservation.setGuestPassport(request.getGuestPassport());
            reservation.setGuestCountry(request.getGuestCountry());
            reservation.setCheckIn(request.getCheckIn());
            reservation.setCheckOut(request.getCheckOut());
            reservation.setGuests(request.getGuests());
            reservation.setRatePlan(request.getRatePlan());

            calculatePrices(reservation, room);

            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setAmountPaid(BigDecimal.ZERO);

            reservationRepository.save(reservation);

            ctx.status(201).json(reservation);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed"));
                return;
            }

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setAmountPaid(reservation.getTotal());
            reservationRepository.update(reservation);

            RoomType roomType = reservationRepository.getRoomType(reservation.getRoomNumber());
            Document document = DocumentService.generateDocument(reservation, roomType);
            emailService.sendConfirmationEmail(reservation.getGuestName(), reservation.getGuestEmail(), document);

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.PENDING &&
                reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Only pending or confirmed reservations can be cancelled"));
                return;
            }

            reservation.setStatus(ReservationStatus.CANCELLED);

            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                reservation.setRefund(reservation.getAmountPaid());
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }

            reservationRepository.update(reservation);

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Only confirmed reservations can be checked in"));
                return;
            }

            reservation.setStatus(ReservationStatus.CHECKED_IN);
            reservationRepository.update(reservation);

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Only checked-in reservations can be checked out"));
                return;
            }

            reservation.setStatus(ReservationStatus.CHECKED_OUT);
            reservationRepository.update(reservation);

            RoomType roomType = reservationRepository.getRoomType(reservation.getRoomNumber());
            Document document = DocumentService.generateDocument(reservation, roomType);

            ctx.json(document);
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() == ReservationStatus.PENDING ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            RoomType roomType = reservationRepository.getRoomType(reservation.getRoomNumber());
            Document document = DocumentService.generateDocument(reservation, roomType);

            ctx.json(document);
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private boolean validateRequest(CreateReservationRequest request, Context ctx) {
        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getCheckIn() == null ||
            request.getCheckOut() == null || request.getGuests() == null ||
            request.getRatePlan() == null) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
            return false;
        }
        return true;
    }

    private boolean validateIdentification(CreateReservationRequest request, Context ctx) {
        boolean hasCpf = request.getGuestCpf() != null;
        boolean hasCnpj = request.getCompanyCnpj() != null || request.getCompanyName() != null;
        boolean hasPassport = request.getGuestPassport() != null || request.getGuestCountry() != null;

        int identificationCount = 0;
        if (hasCpf) identificationCount++;
        if (hasCnpj) identificationCount++;
        if (hasPassport) identificationCount++;

        if (identificationCount != 1) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Exactly one identification method required"));
            return false;
        }

        if (hasCnpj && (request.getCompanyCnpj() == null || request.getCompanyName() == null)) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Both CNPJ and company name are required"));
            return false;
        }

        if (hasPassport && (request.getGuestPassport() == null || request.getGuestCountry() == null)) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Both passport and country are required"));
            return false;
        }

        return true;
    }

    private void calculatePrices(Reservation reservation, Room room) {
        int nights = (int) reservation.getCheckIn().datesUntil(reservation.getCheckOut()).count();
        reservation.setNights(nights);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = reservation.getCheckIn();

        for (int i = 0; i < nights; i++) {
            BigDecimal nightPrice = PricingService.calculateNightPrice(room.getNightlyRate(), currentNight);
            subtotal = subtotal.add(nightPrice);
            currentNight = currentNight.plusDays(1);
        }

        subtotal = PricingService.round(subtotal);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(new BigDecimal("0.15"));
            discount = PricingService.round(discount);
        }
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        total = PricingService.round(total);
        reservation.setTotal(total);
    }
}
