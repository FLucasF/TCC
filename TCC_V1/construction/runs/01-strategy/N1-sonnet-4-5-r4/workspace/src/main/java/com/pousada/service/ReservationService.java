package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.domain.rateplan.RatePlan;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
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

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException("Quarto não encontrado"));

        validateReservation(reservation, room);

        reservation.calculatePricing(room.getNightlyRate());

        return reservationRepository.save(reservation);
    }

    private void validateReservation(Reservation reservation, Room room) {
        if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
            reservation.getCheckOut().equals(reservation.getCheckIn())) {
            throw new InvalidRequestException("Data de saída deve ser posterior à data de entrada");
        }

        if (reservation.getGuests() < 1) {
            throw new InvalidRequestException("Número de hóspedes deve ser no mínimo 1");
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException("Número de hóspedes excede a capacidade do quarto");
        }

        RatePlan ratePlan = reservation.getRatePlanType().create();
        try {
            ratePlan.validate(reservation.getCheckIn(), reservation.getCheckOut(), reservation.getCompanyCode());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        List<Reservation> conflictingReservations = reservationRepository.findByRoomAndDateRange(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );

        if (!conflictingReservations.isEmpty()) {
            throw new RoomUnavailableException("Quarto já reservado para as datas selecionadas");
        }
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada"));
    }

    public Reservation confirm(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.save(reservation);
        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancel(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }
}
