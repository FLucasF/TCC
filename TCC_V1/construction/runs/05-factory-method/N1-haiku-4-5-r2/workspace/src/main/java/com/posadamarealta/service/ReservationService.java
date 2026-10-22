package com.posadamarealta.service;

import com.posadamarealta.model.Reservation;
import com.posadamarealta.model.Room;
import com.posadamarealta.repository.MessageRepository;
import com.posadamarealta.repository.ReservationRepository;
import com.posadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo, MessageRepository messageRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String companyCnpj, String companyName,
                                        String guestPassport, String guestCountry,
                                        LocalDate checkIn, LocalDate checkOut, int guests,
                                        String ratePlan) {
        // Validate room exists
        Room room = roomRepo.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        // Validate dates
        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        int nights = UtilService.calculateNights(checkIn, checkOut);
        if (nights < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Validate guests
        if (guests < 1 || guests > room.capacity) {
            throw new IllegalArgumentException(guests > room.capacity ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        // Validate identification
        int idCount = (guestCpf != null ? 1 : 0) + (companyCnpj != null ? 1 : 0) + (guestPassport != null ? 1 : 0);
        if (idCount != 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Check if company fields are provided when using CNPJ
        if (companyCnpj != null && companyName == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Check if country field is provided when using passport
        if (guestPassport != null && guestCountry == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Check room availability
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            List<Reservation> existing = reservationRepo.findByRoomAndDate(roomNumber, current);
            if (!existing.isEmpty()) {
                throw new IllegalArgumentException("ROOM_UNAVAILABLE");
            }
            current = current.plusDays(1);
        }

        // Calculate price
        BigDecimal subtotal = BigDecimal.ZERO;
        current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = UtilService.calculateNightlyRate(room.nightlyRate, current);
            subtotal = subtotal.add(nightRate);
            current = current.plusDays(1);
        }
        subtotal = UtilService.roundHalfEven(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = UtilService.roundHalfEven(subtotal.multiply(new BigDecimal("0.15")));
        }

        BigDecimal total = UtilService.roundHalfEven(subtotal.subtract(discount));

        return reservationRepo.create(roomNumber, guestName, guestEmail, guestCpf,
            companyCnpj, companyName, guestPassport, guestCountry,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);
    }

    public Reservation confirmReservation(int reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"PENDING".equals(reservation.status)) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservationRepo.confirm(reservationId, reservation.total);

        Reservation updated = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        Room room = roomRepo.findByNumber(reservation.roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        String documentText = DocumentService.generate(updated, room.type).text;

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + reservation.guestName + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            documentText + "\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";

        messageRepo.save("EMAIL", reservation.guestEmail, subject, body);

        return updated;
    }

    public Reservation cancelReservation(int reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!("PENDING".equals(reservation.status) || "CONFIRMED".equals(reservation.status))) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if ("CONFIRMED".equals(reservation.status) && "STANDARD".equals(reservation.ratePlan)) {
            refund = reservation.amountPaid;
        }

        reservationRepo.cancel(reservationId, refund);

        return reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public Reservation checkIn(int reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"CONFIRMED".equals(reservation.status)) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservationRepo.updateStatus(reservationId, "CHECKED_IN");

        return reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public Reservation checkOut(int reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"CHECKED_IN".equals(reservation.status)) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservationRepo.updateStatus(reservationId, "CHECKED_OUT");

        return reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public Reservation getReservation(int reservationId) {
        return reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }
}
