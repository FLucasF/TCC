package com.pousadamarealta.service;

import com.pousadamarealta.document.DocumentGeneratorFactory;
import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;

import java.sql.SQLException;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final EmailService emailService;
    private final DocumentGeneratorFactory documentGeneratorFactory;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            EmailService emailService,
                            DocumentGeneratorFactory documentGeneratorFactory) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.emailService = emailService;
        this.documentGeneratorFactory = documentGeneratorFactory;
    }

    public Reservation createReservation(Reservation reservation) throws SQLException {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (reservation.getGuests() > room.getCapacity()) {
            throw new IllegalArgumentException("Capacity exceeded");
        }

        if (reservationRepository.hasConflict(
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                null)) {
            throw new IllegalArgumentException("Room unavailable");
        }

        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(Long id) throws SQLException {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!reservation.canConfirm()) {
            throw new IllegalStateException("Operation not allowed");
        }

        reservation.confirm(reservation.getTotal(room));
        reservationRepository.save(reservation);

        emailService.sendConfirmationEmail(reservation, room);

        return reservation;
    }

    public Reservation checkIn(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.canCheckIn()) {
            throw new IllegalStateException("Operation not allowed");
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Document checkOut(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!reservation.canCheckOut()) {
            throw new IllegalStateException("Operation not allowed");
        }

        reservation.checkOut();
        reservationRepository.save(reservation);

        return documentGeneratorFactory.getGenerator(reservation).generate(reservation, room);
    }

    public Reservation cancelReservation(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.canCancel()) {
            throw new IllegalStateException("Operation not allowed");
        }

        reservation.cancel();
        return reservationRepository.save(reservation);
    }

    public Document getDocument(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.isPaid()) {
            throw new IllegalStateException("Document not available");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        return documentGeneratorFactory.getGenerator(reservation).generate(reservation, room);
    }
}
