package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.ReservationEvent;
import com.pousadamarealta.notification.ReservationListener;
import com.pousadamarealta.pricing.PricingService;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final PricingService pricingService;
    private final List<ReservationListener> listeners;

    public ReservationService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            PricingService pricingService,
            List<ReservationListener> listeners) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.pricingService = pricingService;
        this.listeners = List.copyOf(listeners);
    }

    public record CreateReservationRequest(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {}

    public Reservation create(CreateReservationRequest request) {
        Room room = roomRepository.findByNumber(request.roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + request.roomNumber));

        if (request.guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (request.guests > room.capacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!request.checkOut.isAfter(request.checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!reservationRepository.isRoomAvailable(request.roomNumber, request.checkIn, request.checkOut, null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        PricingService.PricingResult pricing = pricingService.calculate(
            room, request.checkIn, request.checkOut, request.ratePlan);

        Reservation reservation = new Reservation(
            reservationRepository.nextId(),
            request.roomNumber,
            room.type(),
            request.guestName,
            request.guestEmail,
            request.guestCpf,
            request.checkIn,
            request.checkOut,
            request.guests,
            request.ratePlan,
            pricing.nights(),
            pricing.subtotal(),
            pricing.discount(),
            pricing.total()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirm(int id) {
        Reservation reservation = findById(id);
        reservation.confirm();
        notifyListeners(new ReservationEvent.Confirmed(reservation));
        return reservation;
    }

    public Reservation cancel(int id) {
        Reservation reservation = findById(id);
        boolean wasConfirmed = reservation.status() == ReservationStatus.CONFIRMED;
        reservation.cancel();
        notifyListeners(new ReservationEvent.Cancelled(reservation, wasConfirmed));
        return reservation;
    }

    public Reservation checkIn(int id) {
        Reservation reservation = findById(id);
        reservation.performCheckIn();
        return reservation;
    }

    public Reservation checkOut(int id) {
        Reservation reservation = findById(id);
        reservation.performCheckOut();
        return reservation;
    }

    public Reservation findById(int id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    private void notifyListeners(ReservationEvent event) {
        for (ReservationListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) { super(message); }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) { super(message); }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) { super(message); }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) { super(message); }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) { super(message); }
    }
}
