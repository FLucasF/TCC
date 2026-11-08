package com.marealta.pousada.service;

import com.marealta.pousada.domain.model.Reservation;
import com.marealta.pousada.domain.model.Room;
import com.marealta.pousada.exception.CapacityExceededException;
import com.marealta.pousada.exception.InvalidRequestException;
import com.marealta.pousada.exception.ReservationNotFoundException;
import com.marealta.pousada.exception.RoomUnavailableException;
import com.marealta.pousada.repository.ReservationRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository, RoomService roomService, MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode) {
        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        try {
            List<Reservation> overlapping = reservationRepository.findOverlappingReservations(roomNumber, checkIn, checkOut);
            if (!overlapping.isEmpty()) {
                throw new RoomUnavailableException("Room is not available for the selected dates");
            }

            Reservation reservation = new Reservation(
                null, roomNumber, room.getType(), guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode
            );

            return reservationRepository.save(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating reservation", e);
        }
    }

    public Reservation getReservation(Long id) {
        try {
            return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservation", e);
        }
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.confirm();

        try {
            reservationRepository.update(reservation);
            messageService.sendConfirmationEmail(reservation);
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error confirming reservation", e);
        }
    }

    public Reservation checkInReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.checkIn();

        try {
            reservationRepository.update(reservation);
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking in reservation", e);
        }
    }

    public Reservation checkOutReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.checkOut();

        try {
            reservationRepository.update(reservation);
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking out reservation", e);
        }
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.cancel();

        try {
            reservationRepository.update(reservation);
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error cancelling reservation", e);
        }
    }

    public List<Reservation> getReservationsByNight(LocalDate night) {
        try {
            return reservationRepository.findByNight(night);
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservations", e);
        }
    }

    public List<Reservation> getReservationsByMonth(int year, int month) {
        try {
            return reservationRepository.findByMonth(year, month);
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservations", e);
        }
    }
}
