package br.com.pousada.marealta.service;

import br.com.pousada.marealta.exception.AppException;
import br.com.pousada.marealta.model.*;
import br.com.pousada.marealta.repository.ReservationRepository;
import br.com.pousada.marealta.repository.RoomRepository;
import br.com.pousada.marealta.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final DocumentService documentService;
    private final EmailService emailService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            DocumentService documentService,
                            EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.documentService = documentService;
        this.emailService = emailService;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(AppException::roomNotFound);

        if (reservation.getGuests() < 1) {
            throw AppException.invalidRequest("Número de hóspedes deve ser no mínimo 1");
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw AppException.capacityExceeded();
        }

        if (reservationRepository.hasConflictingReservation(
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                null)) {
            throw AppException.roomUnavailable();
        }

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );
        reservation.setSubtotal(subtotal);

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, reservation.getRatePlan());
        reservation.setDiscount(discount);

        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setRefund(null);

        return reservationRepository.create(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(AppException::reservationNotFound);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw AppException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(AppException::roomNotFound);

        Document document = documentService.generateDocument(reservation, room);
        emailService.sendConfirmationEmail(
            reservation.getGuestEmail(),
            reservation.getGuestName(),
            document
        );

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(AppException::reservationNotFound);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw AppException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(AppException::reservationNotFound);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw AppException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(AppException::reservationNotFound);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw AppException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Document getDocument(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(AppException::reservationNotFound);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw AppException.operationNotAllowed();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(AppException::roomNotFound);

        return documentService.generateDocument(reservation, room);
    }
}
