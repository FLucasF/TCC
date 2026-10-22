package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RatePlanType;
import com.pousadamarealta.exception.CapacityExceededException;
import com.pousadamarealta.exception.RoomUnavailableException;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.time.LocalDate;
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
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlanType ratePlanType, String companyCode) {
        Room room = roomRepository.findByNumber(roomNumber);

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Number of guests exceeds room capacity");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        Reservation reservation = Reservation.create(
            roomNumber, room, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlanType, companyCode
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        reservation.confirm();
        reservationRepository.save(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkInReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOutReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        reservation.cancel();
        return reservationRepository.save(reservation);
    }
}
