package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.time.LocalDate;

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
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room " + roomNumber + " has capacity for " +
                    room.getCapacity() + " guests, but " + guests + " were requested");
        }

        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            if (!reservationRepository.findByRoomAndNight(roomNumber, date).isEmpty()) {
                throw new RoomUnavailableException("Room " + roomNumber + " is already booked for " + date);
            }
            date = date.plusDays(1);
        }

        long id = reservationRepository.getNextId();
        Reservation reservation = new Reservation(
                id, roomNumber, room.getType(), guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, room.getNightlyRate()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.confirm();
            messageService.sendConfirmationEmail(reservation);
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkIn();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkOut();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.cancel();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }
}
