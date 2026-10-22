package com.pousada.marealta.service;

import com.pousada.marealta.dto.CreateReservationRequest;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class ReservationService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static class ValidationError extends Exception {
        public final String code;

        public ValidationError(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    public static Reservation createReservation(CreateReservationRequest req) throws Exception {
        validateRequest(req);

        int roomNumber = req.roomNumber;
        LocalDate checkIn = parseDate(req.checkIn);
        LocalDate checkOut = parseDate(req.checkOut);
        int guests = req.guests;
        String ratePlanStr = req.ratePlan;

        // Validate dates
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationError("INVALID_REQUEST", "Check-out must be after check-in");
        }

        // Check room exists
        var room = RoomRepository.findById(roomNumber)
            .orElseThrow(() -> new ValidationError("ROOM_NOT_FOUND", "Room not found"));

        // Check capacity
        if (guests > room.capacity()) {
            throw new ValidationError("CAPACITY_EXCEEDED", "Too many guests for this room");
        }

        if (guests < 1) {
            throw new ValidationError("INVALID_REQUEST", "At least one guest is required");
        }

        // Check availability
        if (ReservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new ValidationError("ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        // Calculate prices
        RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);
        var priceBreakdown = PriceCalculator.calculatePrice(room.nightlyRate(), checkIn, checkOut, ratePlanStr);

        // Create reservation
        Reservation res = new Reservation();
        res.setRoomNumber(roomNumber);
        res.setGuestName(req.guestName);
        res.setGuestEmail(req.guestEmail);
        res.setGuestCpf(req.guestCpf);
        res.setCompanyCnpj(req.companyCnpj);
        res.setCompanyName(req.companyName);
        res.setGuestPassport(req.guestPassport);
        res.setGuestCountry(req.guestCountry);
        res.setCheckIn(checkIn);
        res.setCheckOut(checkOut);
        res.setGuests(guests);
        res.setRatePlan(ratePlan);
        res.setNights(priceBreakdown.nights());
        res.setSubtotal(priceBreakdown.subtotal());
        res.setDiscount(priceBreakdown.discount());
        res.setTotal(priceBreakdown.total());
        res.setStatus(ReservationStatus.PENDING);
        res.setAmountPaid(BigDecimal.ZERO);
        res.setRefund(null);

        return ReservationRepository.save(res);
    }

    public static Reservation confirm(long reservationId, BigDecimal amountPaid) throws Exception {
        var res = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new ValidationError("RESERVATION_NOT_FOUND", "Reservation not found"));

        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new ValidationError("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        res.setStatus(ReservationStatus.CONFIRMED);
        res.setAmountPaid(amountPaid);
        ReservationRepository.update(res);

        return res;
    }

    public static Reservation checkIn(long reservationId) throws Exception {
        var res = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new ValidationError("RESERVATION_NOT_FOUND", "Reservation not found"));

        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationError("OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        res.setStatus(ReservationStatus.CHECKED_IN);
        ReservationRepository.update(res);

        return res;
    }

    public static Reservation checkOut(long reservationId) throws Exception {
        var res = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new ValidationError("RESERVATION_NOT_FOUND", "Reservation not found"));

        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ValidationError("OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        res.setStatus(ReservationStatus.CHECKED_OUT);
        ReservationRepository.update(res);

        return res;
    }

    public static Reservation cancel(long reservationId) throws Exception {
        var res = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new ValidationError("RESERVATION_NOT_FOUND", "Reservation not found"));

        if (res.getStatus() == ReservationStatus.CHECKED_IN ||
            res.getStatus() == ReservationStatus.CHECKED_OUT ||
            res.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationError("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (res.getStatus() == ReservationStatus.CONFIRMED &&
            res.getRatePlan() == RatePlan.STANDARD) {
            refund = res.getAmountPaid();
        }

        res.setStatus(ReservationStatus.CANCELLED);
        res.setRefund(refund);
        ReservationRepository.update(res);

        return res;
    }

    private static void validateRequest(CreateReservationRequest req) throws ValidationError {
        if (req.roomNumber == null || req.guestName == null || req.guestEmail == null ||
            req.checkIn == null || req.checkOut == null || req.guests == null || req.ratePlan == null) {
            throw new ValidationError("INVALID_REQUEST", "Missing required fields");
        }

        // Validate identification
        int identCount = 0;
        if (req.guestCpf != null) identCount++;
        if (req.companyCnpj != null) identCount++;
        if (req.guestPassport != null) identCount++;

        if (identCount == 0) {
            throw new ValidationError("INVALID_REQUEST", "Identification is required");
        }

        if (identCount > 1) {
            throw new ValidationError("INVALID_REQUEST", "Only one identification type is allowed");
        }

        // If CNPJ is provided, companyName must be provided
        if (req.companyCnpj != null && req.companyName == null) {
            throw new ValidationError("INVALID_REQUEST", "Company name is required with CNPJ");
        }

        // If passport is provided, country must be provided
        if (req.guestPassport != null && req.guestCountry == null) {
            throw new ValidationError("INVALID_REQUEST", "Country is required with passport");
        }
    }

    private static LocalDate parseDate(String dateStr) throws ValidationError {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationError("INVALID_REQUEST", "Invalid date format, use yyyy-MM-dd");
        }
    }
}
