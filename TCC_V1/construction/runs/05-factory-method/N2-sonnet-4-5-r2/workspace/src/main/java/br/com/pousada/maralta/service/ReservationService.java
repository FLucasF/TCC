package br.com.pousada.maralta.service;

import br.com.pousada.maralta.domain.*;
import br.com.pousada.maralta.guesttype.GuestType;
import br.com.pousada.maralta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(
        ReservationRepository reservationRepository,
        RoomService roomService,
        PricingService pricingService,
        MessageService messageService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(
        String roomNumber,
        String guestName,
        String guestEmail,
        GuestType guestType,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {
        Room room = roomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException();
        }

        if (guests < 1) {
            throw new InvalidRequestException();
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        var conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, null);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(checkIn, checkOut, room.getType());
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestType,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }
        return reservation;
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        Document document = reservation.getGuestType().generateDocument(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        messageService.sendConfirmationEmail(reservation.getGuestName(), reservation.getGuestEmail(), document);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setRefund(refundAmount);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Document checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return generateDocument(reservation);
    }

    public Document getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        return generateDocument(reservation);
    }

    private Document generateDocument(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        return reservation.getGuestType().generateDocument(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}
