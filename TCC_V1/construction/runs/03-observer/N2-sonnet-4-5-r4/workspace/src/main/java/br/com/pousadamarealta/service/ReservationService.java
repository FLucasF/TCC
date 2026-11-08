package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.ReservationStatus;
import br.com.pousadamarealta.domain.Room;
import br.com.pousadamarealta.events.EventPublisher;
import br.com.pousadamarealta.events.ReservationCancelledEvent;
import br.com.pousadamarealta.events.ReservationConfirmedEvent;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final EventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
    }

    public Reservation create(String roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) {

        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException();
        }

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests,
            br.com.pousadamarealta.domain.RatePlan.valueOf(ratePlan)
        );

        return reservationRepository.save(reservation);
    }

    public Reservation findById(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }
        return reservation;
    }

    public Reservation confirm(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber());
        BigDecimal total = reservation.calculateTotal(room.getNightlyRate());

        reservation.confirm();
        reservation.setAmountPaid(total);
        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationConfirmedEvent(reservation, total));

        return reservation;
    }

    public Reservation cancel(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        BigDecimal refundAmount = reservation.calculateRefund();

        reservation.cancel();
        reservation.setRefund(refundAmount);
        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationCancelledEvent(reservation, wasConfirmed, refundAmount));

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public String getDocument(Long id) {
        Reservation reservation = findById(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber());
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        return reservation.generateReceipt(room.getType(), subtotal, discount, total);
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
