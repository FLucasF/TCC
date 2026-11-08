package com.pousadamarealta.application.service;

import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import java.time.LocalDate;
import java.util.List;

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
            .orElseThrow(() -> new RoomNotFoundException("Room " + roomNumber + " not found"));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicting.isEmpty()) {
            throw new RoomUnavailableException("Room not available for selected dates");
        }

        Reservation reservation = Reservation.create(roomNumber, guestName, guestEmail, guestCpf,
                                                     checkIn, checkOut, guests, ratePlan);
        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        try {
            reservation.confirm(reservation.calculateTotal(room));
            Reservation saved = reservationRepository.save(reservation);
            eventPublisher.publish(new ReservationConfirmedEvent(saved));
            return saved;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkIn();
            return reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkOut();
            return reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        try {
            reservation.cancel();
            Reservation saved = reservationRepository.save(reservation);
            eventPublisher.publish(new ReservationCancelledEvent(saved, wasConfirmed));
            return saved;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }
}
