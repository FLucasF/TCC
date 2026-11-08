package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.event.EventPublisher;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.exception.AppException;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final EventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            PricingService pricingService,
                            EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.eventPublisher = eventPublisher;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {

        Room room = roomService.getRoomByNumber(roomNumber);

        validateReservation(room, checkIn, checkOut, guests, null);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room.getType(), checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(AppException::reservationNotFound);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw AppException.operationNotAllowed("Só é possível confirmar uma reserva pendente");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        Reservation updated = reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationConfirmedEvent(updated));

        return updated;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw AppException.operationNotAllowed("Não é possível cancelar esta reserva");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD && wasConfirmed) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);
        Reservation updated = reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationCancelledEvent(updated, wasConfirmed));

        return updated;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw AppException.operationNotAllowed("Só é possível fazer check-in de uma reserva confirmada");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw AppException.operationNotAllowed("Só é possível fazer check-out de uma reserva com check-in feito");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    private void validateReservation(Room room, LocalDate checkIn, LocalDate checkOut,
                                    int guests, Long excludeId) {
        if (guests < 1) {
            throw AppException.invalidRequest("A reserva deve ter pelo menos um hóspede");
        }

        if (guests > room.getCapacity()) {
            throw AppException.capacityExceeded();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw AppException.invalidRequest("A data de saída deve ser posterior à data de entrada");
        }

        if (reservationRepository.hasConflictingReservation(room.getNumber(), checkIn, checkOut, excludeId)) {
            throw AppException.roomUnavailable();
        }
    }
}
