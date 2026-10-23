package com.pousadamarealta.service;

import com.pousadamarealta.domain.exception.ApiException;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.messaging.MessageService;

import java.math.BigDecimal;
import java.util.List;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            PricingService pricingService,
                            DocumentService documentService,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.documentService = documentService;
        this.messageService = messageService;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new ApiException("ROOM_NOT_FOUND",
                "Room not found: " + reservation.getRoomNumber(), 404));

        if (reservation.getGuests() > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED",
                "Room capacity exceeded", 422);
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );

        if (!conflicts.isEmpty()) {
            throw new ApiException("ROOM_UNAVAILABLE",
                "Room is not available for the selected dates", 409);
        }

        int nights = pricingService.calculateNights(
            reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal subtotal = pricingService.calculateSubtotal(
            room, reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal discount = pricingService.calculateDiscount(
            subtotal, reservation.getRatePlan());
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException("RESERVATION_NOT_FOUND",
                "Reservation not found", 404));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Cannot confirm reservation in current status", 409);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        Document document = documentService.generateDocument(reservation);
        String documentText = documentService.formatDocument(document);
        String emailBody = documentService.createConfirmationEmail(reservation, documentText);

        Message message = new Message(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
        messageService.send(message);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Cannot cancel reservation in current status", 409);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setRefund(refundAmount);

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Cannot check-in reservation in current status", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Cannot check-out reservation in current status", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Document is only available for paid reservations", 409);
        }

        return documentService.generateDocument(reservation);
    }
}
