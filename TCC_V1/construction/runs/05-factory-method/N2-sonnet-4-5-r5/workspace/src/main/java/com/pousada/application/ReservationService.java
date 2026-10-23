package com.pousada.application;

import com.pousada.domain.guest.GuestType;
import com.pousada.domain.guest.GuestTypeFactory;
import com.pousada.domain.model.*;
import com.pousada.domain.repository.EmailRepository;
import com.pousada.domain.repository.ReservationRepository;
import com.pousada.domain.repository.RoomRepository;
import com.pousada.domain.service.EmailService;
import com.pousada.domain.service.PricingService;

import java.time.LocalDate;

public final class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final EmailRepository emailRepository;
    private final PricingService pricingService;
    private final EmailService emailService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            EmailRepository emailRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.emailRepository = emailRepository;
        this.pricingService = new PricingService();
        this.emailService = new EmailService();
    }

    public Reservation createReservation(
            String roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            String companyCnpj,
            String companyName,
            String guestPassport,
            String guestCountry,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) {

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("At least 1 guest is required");
        }

        if (guests > room.capacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut).isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        GuestType guestType = GuestTypeFactory.create(guestCpf, companyCnpj, companyName, guestPassport, guestCountry);

        PricingService.PriceBreakdown pricing = pricingService.calculate(room.type(), checkIn, checkOut, ratePlan);

        Reservation reservation = Reservation.builder()
            .roomNumber(roomNumber)
            .roomType(room.type())
            .guestName(guestName)
            .guestEmail(guestEmail)
            .guestType(guestType)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .guests(guests)
            .ratePlan(ratePlan)
            .nights(pricing.nights)
            .subtotal(pricing.subtotal)
            .discount(pricing.discount)
            .total(pricing.total)
            .build();

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canBeConfirmed()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.status());
        }

        reservation.confirm();
        reservationRepository.update(reservation);

        Document document = reservation.generateDocument();
        Email email = emailService.createConfirmationEmail(reservation, document);
        emailRepository.save(email);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canBeCancelled()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.status());
        }

        reservation.cancel();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check-in reservation in status " + reservation.status());
        }

        reservation.doCheckIn();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Document checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check-out reservation in status " + reservation.status());
        }

        reservation.doCheckOut();
        reservationRepository.update(reservation);

        return reservation.generateDocument();
    }

    public Document getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        return reservation.generateDocument();
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
