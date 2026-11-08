package com.pousadamareaita.service;

import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.Room;
import com.pousadamareaita.repository.ReservationRepository;
import com.pousadamareaita.repository.RoomRepository;
import com.pousadamareaita.util.PricingCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final ReservationRepository reservationRepo = new ReservationRepository();
    private static final RoomRepository roomRepo = new RoomRepository();
    private static final Map<String, Boolean> VALID_RATE_PLANS = Map.of(
        "STANDARD", true,
        "NON_REFUNDABLE", true,
        "CORPORATE", true,
        "WEEKEND_PACKAGE", true,
        "LONG_STAY", true
    );

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                         int guests, String ratePlan, String companyCode) {

        Optional<Room> roomOpt = roomRepo.findByNumber(roomNumber);
        if (roomOpt.isEmpty()) {
            throw new ApiException("ROOM_NOT_FOUND", "Room not found", 404);
        }

        Room room = roomOpt.get();

        if (!VALID_RATE_PLANS.getOrDefault(ratePlan, false)) {
            throw new ApiException("INVALID_REQUEST", "Invalid rate plan", 400);
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity", 422);
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
        }

        validateRatePlanRules(checkIn, checkOut, ratePlan, companyCode, room);

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException("ROOM_UNAVAILABLE", "Room is unavailable for selected dates", 409);
        }

        PricingCalculator.PricingResult pricing = PricingCalculator.calculatePrice(
            roomNumber, checkIn, checkOut, ratePlan, companyCode, room.getNightlyRate()
        );

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
            ratePlan, companyCode, pricing.nights, pricing.subtotal, pricing.discount, pricing.total
        );

        return reservationRepo.save(reservation);
    }

    public Reservation confirmReservation(long reservationId, java.math.BigDecimal amountPaid) {
        Optional<Reservation> reservationOpt = reservationRepo.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only confirm pending reservations", 409);
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationRepo.update(reservation);

        EmailService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long reservationId) {
        Optional<Reservation> reservationOpt = reservationRepo.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() == Reservation.Status.CHECKED_IN ||
            reservation.getStatus() == Reservation.Status.CHECKED_OUT ||
            reservation.getStatus() == Reservation.Status.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation", 409);
        }

        java.math.BigDecimal refund = PricingCalculator.calculateRefund(
            reservation.getRatePlan(), reservation.getAmountPaid()
        );

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservation.setRefund(refund);
        reservationRepo.update(reservation);

        return reservation;
    }

    public Reservation checkIn(long reservationId) {
        Optional<Reservation> reservationOpt = reservationRepo.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check-in confirmed reservations", 409);
        }

        reservation.setStatus(Reservation.Status.CHECKED_IN);
        reservationRepo.update(reservation);

        return reservation;
    }

    public Reservation checkOut(long reservationId) {
        Optional<Reservation> reservationOpt = reservationRepo.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check-out from checked-in state", 409);
        }

        reservation.setStatus(Reservation.Status.CHECKED_OUT);
        reservationRepo.update(reservation);

        return reservation;
    }

    public Optional<Reservation> getReservation(long reservationId) {
        return reservationRepo.findById(reservationId);
    }

    private void validateRatePlanRules(LocalDate checkIn, LocalDate checkOut, String ratePlan,
                                      String companyCode, Room room) {
        if ("CORPORATE".equals(ratePlan)) {
            if (companyCode == null || !isCorporateCodeValid(companyCode)) {
                throw new ApiException("INVALID_REQUEST", "Invalid or missing company code", 400);
            }
        } else if ("WEEKEND_PACKAGE".equals(ratePlan)) {
            if (!isWeekendPackage(checkIn, checkOut)) {
                throw new ApiException("INVALID_REQUEST", "Weekend package must be Friday to Sunday (2 nights)", 400);
            }
        } else if ("LONG_STAY".equals(ratePlan)) {
            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights < 7) {
                throw new ApiException("INVALID_REQUEST", "Long stay requires minimum 7 nights", 400);
            }
        }
    }

    private boolean isCorporateCodeValid(String code) {
        return code.equals("TECNOSUL") || code.equals("AGROVALE") || code.equals("FARMAVIDA");
    }

    private boolean isWeekendPackage(LocalDate checkIn, LocalDate checkOut) {
        java.time.DayOfWeek inDay = checkIn.getDayOfWeek();
        java.time.DayOfWeek outDay = checkOut.getDayOfWeek();
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        return inDay == java.time.DayOfWeek.FRIDAY &&
               outDay == java.time.DayOfWeek.SUNDAY &&
               nights == 2;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            List<Reservation> reservations = reservationRepo.findByRoomAndDate(roomNumber, current);
            if (!reservations.isEmpty()) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    public static class ApiException extends RuntimeException {
        public final String code;
        public final int statusCode;

        public ApiException(String code, String message, int statusCode) {
            super(message);
            this.code = code;
            this.statusCode = statusCode;
        }
    }
}
