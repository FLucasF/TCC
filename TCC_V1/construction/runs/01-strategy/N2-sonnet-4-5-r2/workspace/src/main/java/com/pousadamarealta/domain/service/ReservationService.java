package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.service.rateplan.CorporateRatePlan;
import com.pousadamarealta.domain.service.rateplan.RatePlan;
import com.pousadamarealta.domain.service.rateplan.RatePlanRegistry;
import com.pousadamarealta.infrastructure.database.ReservationRepository;
import com.pousadamarealta.infrastructure.database.RoomRepository;

import java.math.BigDecimal;
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
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Número de hóspedes deve ser no mínimo 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Data de saída deve ser posterior à data de entrada");
        }

        RatePlan ratePlan = RatePlanRegistry.get(ratePlanType);

        try {
            ratePlan.validate(checkIn, checkOut, companyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        BigDecimal subtotal = ratePlan.calculateSubtotal(room.getType(), checkIn, checkOut);
        BigDecimal discount;

        if (ratePlanType == RatePlanType.CORPORATE && ratePlan instanceof CorporateRatePlan corporatePlan) {
            discount = corporatePlan.calculateDiscount(subtotal, companyCode);
        } else {
            discount = ratePlan.calculateDiscount(subtotal, room.getType(), checkIn, checkOut);
        }

        BigDecimal total = subtotal.subtract(discount);

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlanType, companyCode,
            subtotal, discount, total
        );

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

        Reservation saved = reservationRepository.update(reservation);
        messageService.sendConfirmationEmail(saved);

        return saved;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            RatePlan ratePlan = RatePlanRegistry.get(reservation.getRatePlan());
            BigDecimal refundAmount = ratePlan.calculateRefund(reservation.getAmountPaid());
            reservation.cancel(refundAmount);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.update(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.update(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.update(reservation);
    }

    public List<Reservation> findReservationsForNight(LocalDate night) {
        return reservationRepository.findPaidReservationsForNight(night);
    }

    public List<Reservation> findReservationsForMonth(int year, int month) {
        return reservationRepository.findReservationsStartingInMonth(year, month);
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        List<Reservation> existingReservations = reservationRepository.findActiveReservationsByRoom(roomNumber);

        return existingReservations.stream()
            .filter(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId))
            .anyMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
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
