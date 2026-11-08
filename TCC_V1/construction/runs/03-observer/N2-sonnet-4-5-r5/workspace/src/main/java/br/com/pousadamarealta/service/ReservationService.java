package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.*;
import br.com.pousadamarealta.domain.events.ReservationCancelled;
import br.com.pousadamarealta.domain.events.ReservationConfirmed;
import br.com.pousadamarealta.domain.events.ReservationEventListener;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final List<ReservationEventListener> listeners;

    public ReservationService(RoomRepository roomRepository,
                             ReservationRepository reservationRepository,
                             List<ReservationEventListener> listeners) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        Reservation reservation = new Reservation(
            null, room, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan
        );

        checkRoomAvailability(roomNumber, checkIn, checkOut);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.confirm();
        reservation = reservationRepository.save(reservation);

        for (ReservationEventListener listener : listeners) {
            listener.onReservationConfirmed(new ReservationConfirmed(reservation));
        }

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);
        reservation.performCheckIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);
        reservation.performCheckOut();
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);
        ReservationStatus previousStatus = reservation.status();
        reservation.cancel();
        reservation = reservationRepository.save(reservation);

        for (ReservationEventListener listener : listeners) {
            listener.onReservationCancelled(new ReservationCancelled(reservation, previousStatus));
        }

        return reservation;
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    private void checkRoomAvailability(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            List<Reservation> conflicting = reservationRepository.findByRoomAndNight(
                roomNumber, date, ReservationStatus.CANCELLED
            );
            if (!conflicting.isEmpty()) {
                throw new RoomUnavailableException(roomNumber);
            }
            date = date.plusDays(1);
        }
    }
}
