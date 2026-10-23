package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.messaging.MessagingService;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessagingService messagingService;
    private final DocumentService documentService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessagingService messagingService,
                            DocumentService documentService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messagingService = messagingService;
        this.documentService = documentService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = Reservation.createNew(null, roomNumber, guestName, guestEmail,
                                                        guestCpf, checkIn, checkOut, guests,
                                                        ratePlan, room.getType().getNightlyRate());

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        Reservation saved = reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(saved.getRoomNumber()).orElseThrow();
        String roomType = room.getType().name();
        String emailBody = documentService.generateConfirmationEmail(saved, roomType);

        messagingService.sendEmail(saved.getGuestEmail(),
                                  "Reserva confirmada - Pousada Maré Alta",
                                  emailBody);

        return saved;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
