package com.pousada.maralta.service;

import com.pousada.maralta.domain.*;
import com.pousada.maralta.repository.MessageRepository;
import com.pousada.maralta.repository.ReservationRepository;
import com.pousada.maralta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;
    private final DocumentGenerator documentGenerator;
    private final EmailService emailService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageRepository messageRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.pricingService = new PricingService();
        this.documentGenerator = new DocumentGenerator();
        this.emailService = new EmailService();
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        GuestIdentification identification, LocalDate checkIn,
                                        LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1 || guests > room.capacity()) {
            throw new CapacityExceededException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            List<Reservation> conflicts = reservationRepository.findByRoomAndNight(roomNumber, current);
            if (!conflicts.isEmpty()) {
                throw new RoomUnavailableException();
            }
            current = current.plusDays(1);
        }

        var subtotal = pricingService.calculateSubtotal(room.type(), checkIn, checkOut);
        var discount = pricingService.calculateDiscount(subtotal, ratePlan);
        var total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            null, roomNumber, guestName, guestEmail, identification,
            checkIn, checkOut, guests, ratePlan, nights,
            subtotal, discount, total, ReservationStatus.PENDING,
            null, null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ReservationDocument document = documentGenerator.generate(reservation, room);
        Message message = emailService.createConfirmationEmail(reservation, document);
        messageRepository.save(message);

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

    public ReservationDocument checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return documentGenerator.generate(reservation, room);
    }

    public ReservationDocument getDocument(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return documentGenerator.generate(reservation, room);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}
