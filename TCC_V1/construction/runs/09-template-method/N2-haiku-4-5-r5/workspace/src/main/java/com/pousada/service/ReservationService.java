package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                               String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                               int guests, RatePlan ratePlan) {
        Room room = RoomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Room not found", 404);
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
        }

        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "At least one guest is required", 400);
        }

        if (guests > room.capacity) {
            throw new ApiException("CAPACITY_EXCEEDED", "Too many guests for this room", 422);
        }

        List<Reservation> conflicts = ReservationRepository.findByRoomAndDates(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new ApiException("ROOM_UNAVAILABLE", "Room not available for these dates", 409);
        }

        PricingService.PricingResult pricing = PricingService.calculatePrice(room, checkIn, checkOut, ratePlan);

        return ReservationRepository.create(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan,
                pricing.nights, pricing.subtotal, pricing.discount, pricing.total
        );
    }

    public static Reservation confirmReservation(int id, BigDecimal amountPaid) {
        Reservation reservation = ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (reservation.status != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only confirm pending reservations", 409);
        }

        ReservationRepository.updateStatus(id, ReservationStatus.CONFIRMED);
        ReservationRepository.updateAmountPaid(id, amountPaid);

        Reservation confirmed = ReservationRepository.findById(id);

        sendConfirmationEmail(confirmed);

        return confirmed;
    }

    public static Reservation cancelReservation(int id) {
        Reservation reservation = ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (reservation.status != ReservationStatus.PENDING && reservation.status != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only cancel pending or confirmed reservations", 409);
        }

        ReservationRepository.updateStatus(id, ReservationStatus.CANCELLED);

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.status == ReservationStatus.CONFIRMED) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                refund = reservation.amountPaid;
            }
        }

        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            ReservationRepository.updateRefund(id, refund);
        }

        return ReservationRepository.findById(id);
    }

    public static Reservation checkIn(int id) {
        Reservation reservation = ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check in confirmed reservations", 409);
        }

        ReservationRepository.updateStatus(id, ReservationStatus.CHECKED_IN);
        return ReservationRepository.findById(id);
    }

    public static Reservation checkOut(int id) {
        Reservation reservation = ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check out from checked in reservations", 409);
        }

        ReservationRepository.updateStatus(id, ReservationStatus.CHECKED_OUT);
        return ReservationRepository.findById(id);
    }

    public static Reservation getReservation(int id) {
        Reservation reservation = ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
        return reservation;
    }

    private static void sendConfirmationEmail(Reservation reservation) {
        Room room = RoomRepository.findByNumber(reservation.roomNumber);

        String comprovante = DocumentService.generateComprovante(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + reservation.guestName + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                comprovante + "\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        MessageRepository.create("EMAIL", reservation.guestEmail, subject, body);
    }
}
