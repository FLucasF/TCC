package br.com.maralta.service;

import br.com.maralta.exception.ApiException;
import br.com.maralta.model.*;
import br.com.maralta.repository.ReservationRepository;
import br.com.maralta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;
    private final EmailService emailService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            PricingService pricingService,
            DocumentService documentService,
            EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.documentService = documentService;
        this.emailService = emailService;
    }

    public Reservation createReservation(Reservation reservation) {
        validate(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(ApiException::roomNotFound);

        if (reservation.getGuests() > room.getCapacity()) {
            throw ApiException.capacityExceeded();
        }

        checkAvailability(reservation.getRoomNumber(), reservation.getCheckIn(),
                         reservation.getCheckOut(), null);

        calculatePricing(reservation, room);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(ApiException::reservationNotFound);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw ApiException.operationNotAllowed("Cannot confirm reservation in current status");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = documentService.generateDocument(reservation, room);
        emailService.sendConfirmationEmail(reservation.getGuestEmail(), reservation.getGuestName(), document);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw ApiException.operationNotAllowed("Cannot check-in reservation in current status");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw ApiException.operationNotAllowed("Cannot check-out reservation in current status");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return documentService.generateDocument(reservation, room);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw ApiException.operationNotAllowed("Cannot cancel reservation in current status");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = calculateRefund(reservation);
        reservation.setRefund(refundAmount);

        return reservationRepository.save(reservation);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw ApiException.operationNotAllowed("Document only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return documentService.generateDocument(reservation, room);
    }

    private void validate(Reservation reservation) {
        if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
            reservation.getCheckOut().equals(reservation.getCheckIn())) {
            throw ApiException.invalidRequest("Check-out must be after check-in");
        }

        if (reservation.getGuests() < 1) {
            throw ApiException.invalidRequest("At least one guest is required");
        }

        if (reservation.getIdentification() == null || !reservation.getIdentification().isValid()) {
            throw ApiException.invalidRequest("Invalid guest identification");
        }
    }

    private void checkAvailability(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        var conflicting = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, excludeId);
        if (!conflicting.isEmpty()) {
            throw ApiException.roomUnavailable();
        }
    }

    private void calculatePricing(Reservation reservation, Room room) {
        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, reservation.getRatePlan());
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        reservation.setTotal(total);
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }

        return reservation.getAmountPaid();
    }
}
