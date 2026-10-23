package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
                                        int guests, String ratePlanStr) {
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

        com.pousada.domain.RatePlan ratePlan;
        try {
            ratePlan = com.pousada.domain.RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan");
        }

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan
        );

        BigDecimal subtotal = calculateSubtotal(reservation, room);
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal total = subtotal.subtract(discount);

        reservation.setAmountPaid(BigDecimal.ZERO);

        return reservationRepository.save(reservation);
    }

    private BigDecimal calculateSubtotal(Reservation reservation, Room room) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = reservation.getCheckIn();

        while (current.isBefore(reservation.getCheckOut())) {
            BigDecimal nightPrice = reservation.calculateNightPrice(current, room.getNightlyRate());
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Optional<Reservation> getReservation(Long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException());

        BigDecimal subtotal = calculateSubtotal(reservation, room);
        BigDecimal discount = subtotal.multiply(reservation.getRatePlan().getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal total = subtotal.subtract(discount);

        try {
            reservation.confirm(subtotal, total);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        reservation.setRefund(BigDecimal.ZERO);

        reservation = reservationRepository.save(reservation);

        String document = messageService.formatDocument(reservation, room.getType().name());
        messageService.sendConfirmationEmail(reservation, document);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        try {
            reservation.checkOut();
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
