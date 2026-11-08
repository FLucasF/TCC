package br.com.marealta.service;

import br.com.marealta.domain.*;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             PricingService pricingService,
                             DocumentService documentService,
                             MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.documentService = documentService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(
                roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
                null,
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
                BigDecimal.ZERO,
                null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        Document document = documentService.generateDocument(reservation, room);
        messageService.sendConfirmationEmail(reservation.getGuestEmail(),
                reservation.getGuestName(), document);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        return documentService.generateDocument(reservation, room);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD
                && reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        return documentService.generateDocument(reservation, room);
    }

    public static class RoomNotFoundException extends RuntimeException {
    }

    public static class ReservationNotFoundException extends RuntimeException {
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
    }

    public static class RoomUnavailableException extends RuntimeException {
    }

    public static class OperationNotAllowedException extends RuntimeException {
    }
}
