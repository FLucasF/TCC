package br.com.marealta.service;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.pricing.PricingService;
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
    private final MessageService messageService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            PricingService pricingService,
            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(Reservation reservation) {
        validateReservation(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException());

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, reservation.getRatePlan());
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = switch (reservation.getRatePlan()) {
            case STANDARD -> reservation.getAmountPaid();
            case NON_REFUNDABLE -> BigDecimal.ZERO;
        };

        reservation.setRefund(refundAmount);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation findReservationById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    private void validateReservation(Reservation reservation) {
        if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
            reservation.getCheckOut().isEqual(reservation.getCheckIn())) {
            throw new InvalidRequestException();
        }

        if (reservation.getGuests() < 1) {
            throw new InvalidRequestException();
        }
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {}
}
