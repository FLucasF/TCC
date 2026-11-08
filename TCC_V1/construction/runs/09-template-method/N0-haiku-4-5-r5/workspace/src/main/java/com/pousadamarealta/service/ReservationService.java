package com.pousadamarealta.service;

import com.pousadamarealta.model.Document;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final MessageService messageService = new MessageService();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) throws Exception {
        // Validate room exists
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new ValidationException("ROOM_NOT_FOUND", "Room not found"));

        // Validate guests
        if (guests < 1 || guests > room.capacity) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Guests exceed room capacity");
        }

        // Validate dates
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("INVALID_REQUEST", "Check-out must be after check-in");
        }

        // Check room availability
        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut)) {
            throw new ValidationException("ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        // Calculate pricing
        int nights = PricingService.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PricingService.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = PricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingService.calculateTotal(subtotal, discount);

        // Create reservation
        return reservationRepository.create(roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);
    }

    public Reservation getReservation(int id) throws Exception {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found"));
    }

    public Reservation confirmReservation(int id) throws Exception {
        Reservation res = getReservation(id);

        if (!"PENDING".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        reservationRepository.updateStatus(id, "CONFIRMED");
        reservationRepository.updateAmountPaid(id, res.total);

        Reservation updated = getReservation(id);

        // Get room info for document
        Room room = roomRepository.findByNumber(res.roomNumber).orElseThrow();
        Document doc = new Document(res.id, res.guestName, res.guestCpf, res.roomNumber, room.type,
            res.checkIn, res.checkOut, res.nights, res.subtotal, res.discount, res.total);

        // Send confirmation email
        messageService.sendConfirmationEmail(res, doc);

        return updated;
    }

    public Reservation cancelReservation(int id) throws Exception {
        Reservation res = getReservation(id);

        if ("CHECKED_IN".equals(res.status) || "CHECKED_OUT".equals(res.status) || "CANCELLED".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current status");
        }

        reservationRepository.updateStatus(id, "CANCELLED");

        // Calculate refund
        BigDecimal refund = BigDecimal.ZERO;
        if ("CONFIRMED".equals(res.status)) {
            if ("STANDARD".equals(res.ratePlan)) {
                refund = res.amountPaid;
            }
        }
        // PENDING has no refund as nothing was paid

        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            reservationRepository.updateRefund(id, refund);
        }

        return getReservation(id);
    }

    public Reservation checkInReservation(int id) throws Exception {
        Reservation res = getReservation(id);

        if (!"CONFIRMED".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        reservationRepository.updateStatus(id, "CHECKED_IN");
        return getReservation(id);
    }

    public Reservation checkOutReservation(int id) throws Exception {
        Reservation res = getReservation(id);

        if (!"CHECKED_IN".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        reservationRepository.updateStatus(id, "CHECKED_OUT");
        return getReservation(id);
    }

    public Document getDocument(int id) throws Exception {
        Reservation res = getReservation(id);

        if ("PENDING".equals(res.status) || "CANCELLED".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Document only available for confirmed or checked-in reservations");
        }

        Room room = roomRepository.findByNumber(res.roomNumber).orElseThrow();
        return new Document(res.id, res.guestName, res.guestCpf, res.roomNumber, room.type,
            res.checkIn, res.checkOut, res.nights, res.subtotal, res.discount, res.total);
    }

    public static class ValidationException extends Exception {
        public String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}
