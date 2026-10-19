package com.pousada.domain.service;

import com.pousada.domain.model.*;
import com.pousada.domain.pricing.PricingCalculator;
import com.pousada.domain.validation.ReservationValidator;
import com.pousada.domain.validation.ValidationException;
import com.pousada.infrastructure.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;

    public ReservationService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber);

        ReservationValidator.validateDates(checkIn, checkOut);
        ReservationValidator.validateGuests(guests, room);

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            List<Reservation> conflicting = reservationRepository
                .findActiveByRoomAndNight(roomNumber, current);
            ReservationValidator.validateRoomAvailability(roomNumber, checkIn, checkOut, conflicting);
            current = current.plusDays(1);
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getType(), checkIn, checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        Guest guest = new Guest(guestName, guestEmail, guestCpf);
        Long id = reservationRepository.nextId();

        Reservation reservation = new Reservation(id, roomNumber, guest, checkIn, checkOut,
                                                  guests, ratePlan, nights, subtotal,
                                                  discount, total);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND",
                "Reservation " + id + " not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);
        ReservationValidator.validateConfirmation(reservation);
        reservation.confirm();
        return reservation;
    }

    public Reservation checkInReservation(Long id) {
        Reservation reservation = getReservation(id);
        ReservationValidator.validateCheckIn(reservation);
        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOutReservation(Long id) {
        Reservation reservation = getReservation(id);
        ReservationValidator.validateCheckOut(reservation);
        reservation.checkOut();
        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);
        ReservationValidator.validateCancellation(reservation);
        reservation.cancel();
        return reservation;
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}
