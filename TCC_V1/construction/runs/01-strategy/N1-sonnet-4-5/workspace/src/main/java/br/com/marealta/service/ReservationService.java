package br.com.marealta.service;

import br.com.marealta.domain.*;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;

import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                       String guestCpf, DateRange dateRange, int guests,
                                       String ratePlanType, String companyCode) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        Reservation reservation = new Reservation(null, roomNumber, room.getType(), guestName,
                                                 guestEmail, guestCpf, dateRange, guests,
                                                 ratePlanType, companyCode);

        reservation.validate();

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, dateRange, null);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.confirm();
        reservationRepository.update(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.cancel();
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);
        reservation.checkIn();
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);
        reservation.checkOut();
        reservationRepository.update(reservation);
        return reservation;
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
}
