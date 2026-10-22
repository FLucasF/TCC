package com.pousadamarealta.service;

import com.pousadamarealta.domain.enums.RatePlan;
import com.pousadamarealta.domain.enums.ReservationStatus;
import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.event.EventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

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

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("Guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(room.getCapacity(), guests);
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException(roomNumber);
        }

        BigDecimal nightlyRate = calculateNightlyRate(room.getNightlyRate(), checkIn, checkOut);

        Reservation reservation = Reservation.create(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nightlyRate
        );

        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(Long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.confirm();
        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationConfirmedEvent(reservation));

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only pending or confirmed reservations can be cancelled");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        reservation.cancel();
        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationCancelledEvent(reservation, wasConfirmed));

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    private BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal totalRate = BigDecimal.ZERO;
        int nights = 0;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal rate = baseRate;
            if (isHighSeason(current)) {
                rate = rate.multiply(new BigDecimal("1.20"))
                          .setScale(2, RoundingMode.HALF_EVEN);
            }
            totalRate = totalRate.add(rate);
            nights++;
            current = current.plusDays(1);
        }

        return totalRate.divide(new BigDecimal(nights), 2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(Long id) {
            super("Reservation not found: " + id);
        }
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String number) {
            super("Room not found: " + number);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String roomNumber) {
            super("Room unavailable: " + roomNumber);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(int capacity, int guests) {
            super("Capacity exceeded: " + capacity + " < " + guests);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
