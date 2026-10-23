package br.com.marealta.service;

import br.com.marealta.exception.*;
import br.com.marealta.model.*;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;

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

    public Reservation createReservation(
            String roomNumber,
            String guestName,
            String guestEmail,
            GuestIdentification guestIdentification,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) {

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(
                String.format("Room %s has capacity for %d guests, but %d were requested",
                    roomNumber, room.getCapacity(), guests)
            );
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException(
                String.format("Room %s is not available for the selected dates", roomNumber)
            );
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestIdentification(guestIdentification);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long reservationId) {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException(
                "Only pending reservations can be confirmed"
            );
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        Reservation updated = reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String documentText = documentService.generateDocument(updated, room);
        emailService.sendConfirmationEmail(updated, documentText);

        return updated;
    }

    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException(
                "Cannot cancel reservation in status " + reservation.getStatus()
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setRefund(refundAmount);

        return reservationRepository.update(reservation);
    }

    public Reservation checkIn(Long reservationId) {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException(
                "Only confirmed reservations can be checked in"
            );
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.update(reservation);
    }

    public Reservation checkOut(Long reservationId) {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException(
                "Only checked-in reservations can be checked out"
            );
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.update(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    private boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservationRepository.findConflictingReservations(roomNumber, checkIn, checkOut)
                .isEmpty();
    }
}
