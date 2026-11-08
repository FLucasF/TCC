package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.rateplan.CorporateRatePlan;
import com.pousadamarealta.rateplan.PricingResult;
import com.pousadamarealta.rateplan.RatePlanRegistry;
import com.pousadamarealta.rateplan.RatePlanStrategy;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan, String companyCode) {

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(guests, room.getCapacity());
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidRequestException("Check-out date must be after check-in date");
        }

        RatePlanStrategy strategy = RatePlanRegistry.get(ratePlan);
        strategy.validate(room, checkIn, checkOut, guests, companyCode);

        List<Reservation> conflicting = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, null);
        if (!conflicting.isEmpty()) {
            throw new RoomUnavailableException(roomNumber);
        }

        PricingResult pricing;
        if (ratePlan == RatePlan.CORPORATE && strategy instanceof CorporateRatePlan corporateStrategy) {
            pricing = corporateStrategy.calculatePriceWithCompanyCode(room, checkIn, checkOut, companyCode);
        } else {
            pricing = strategy.calculatePrice(room, checkIn, checkOut);
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setCompanyCode(companyCode);
        reservation.calculateNights();
        reservation.setSubtotal(pricing.getSubtotal());
        reservation.setDiscount(pricing.getDiscount());
        reservation.setTotal(pricing.getTotal());

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String receiptText = notificationService.generateReceipt(reservation, room.getType().name());
        notificationService.sendConfirmationEmail(reservation, receiptText);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only pending or confirmed reservations can be cancelled");
        }

        RatePlanStrategy strategy = RatePlanRegistry.get(reservation.getRatePlan());
        java.math.BigDecimal refundAmount = strategy.calculateRefund(reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }
}
