package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.events.EventPublisher;
import com.pousadamarealta.events.ReservationEvent;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final EventPublisher eventPublisher;
    private final ReceiptService receiptService;

    public ReservationService(RoomRepository roomRepository, ReservationRepository reservationRepository,
                             MessageRepository messageRepository, EventPublisher eventPublisher,
                             ReceiptService receiptService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.receiptService = receiptService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException(guests > room.getCapacity() ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            if (!reservationRepository.findByRoomAndDate(roomNumber, date).iterator().hasNext()) {
                continue;
            }
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        long reservationId = reservationRepository.getNextId();
        Reservation reservation = new Reservation(reservationId, roomNumber, guestName, guestEmail, guestCpf,
                                                 checkIn, checkOut, guests, ratePlan, subtotal, discount, total);
        reservationRepository.save(reservation);
        return reservation;
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        String recipientEmail = reservation.getGuestEmail();
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = receiptService.generateConfirmationEmail(reservation);

        Message guestMessage = new Message(
            messageRepository.getNextMessageId(),
            Message.MessageChannel.EMAIL,
            recipientEmail,
            subject,
            body
        );
        messageRepository.save(guestMessage);

        ReservationStatus previousStatus = ReservationStatus.PENDING;
        eventPublisher.publish(new ReservationEvent(ReservationEvent.Type.CONFIRMED, reservation, previousStatus));

        return reservation;
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        ReservationStatus previousStatus = reservation.getStatus();
        if (previousStatus != ReservationStatus.PENDING && previousStatus != ReservationStatus.CONFIRMED &&
            previousStatus != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationStatus beforeCancellation = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CANCELLED);

        if (previousStatus == ReservationStatus.CONFIRMED || previousStatus == ReservationStatus.CHECKED_IN) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                reservation.setRefund(reservation.getAmountPaid());
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        if (beforeCancellation != ReservationStatus.PENDING) {
            eventPublisher.publish(new ReservationEvent(ReservationEvent.Type.CANCELLED, reservation, beforeCancellation));
        }

        return reservation;
    }

    public String getReceipt(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (!reservation.isPaid()) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        return receiptService.generateReceipt(reservation);
    }
}
