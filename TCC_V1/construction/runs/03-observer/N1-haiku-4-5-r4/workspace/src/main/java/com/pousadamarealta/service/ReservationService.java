package com.pousadamarealta.service;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.models.RatePlan;
import com.pousadamarealta.models.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.notifications.NotificationService;
import com.pousadamarealta.utils.PricingUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReservationService(
        ReservationRepository reservationRepository,
        RoomRepository roomRepository,
        MessageRepository messageRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = new NotificationService(messageRepository);
    }

    public Reservation createReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) throws Exception {
        validateReservationInput(roomNumber, checkIn, checkOut, guests);

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new Exception("ROOM_NOT_FOUND"));

        if (guests > room.capacity) {
            throw new Exception("CAPACITY_EXCEEDED");
        }

        List<Reservation> conflictingReservations = reservationRepository
            .findByRoomAndDates(roomNumber, checkIn, checkOut);

        if (!conflictingReservations.isEmpty()) {
            throw new Exception("ROOM_UNAVAILABLE");
        }

        int nights = PricingUtils.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PricingUtils.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = PricingUtils.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PricingUtils.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation();
        reservation.roomNumber = roomNumber;
        reservation.roomType = room.type;
        reservation.guestName = guestName;
        reservation.guestEmail = guestEmail;
        reservation.guestCpf = guestCpf;
        reservation.checkIn = checkIn;
        reservation.checkOut = checkOut;
        reservation.guests = guests;
        reservation.ratePlan = ratePlan;
        reservation.nights = nights;
        reservation.subtotal = subtotal;
        reservation.discount = discount;
        reservation.total = total;
        reservation.status = ReservationStatus.PENDING;
        reservation.amountPaid = BigDecimal.ZERO;

        return reservationRepository.create(reservation);
    }

    public Reservation confirmReservation(long id) throws Exception {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.PENDING) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CONFIRMED;
        reservation.amountPaid = reservation.total;

        reservationRepository.update(reservation);
        notificationService.notifyConfirmation(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) throws Exception {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(long id) throws Exception {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CHECKED_OUT;
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) throws Exception {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.PENDING &&
            reservation.status != ReservationStatus.CONFIRMED) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        boolean wasConfirmed = reservation.status == ReservationStatus.CONFIRMED;

        if (wasConfirmed) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                reservation.refund = reservation.amountPaid;
            } else {
                reservation.refund = BigDecimal.ZERO;
            }
        } else {
            reservation.refund = BigDecimal.ZERO;
        }

        reservation.status = ReservationStatus.CANCELLED;
        reservationRepository.update(reservation);

        if (wasConfirmed) {
            notificationService.notifyCancellation(reservation);
        }

        return reservation;
    }

    public Optional<Reservation> getReservation(long id) throws Exception {
        return reservationRepository.findById(id);
    }

    private void validateReservationInput(
        int roomNumber,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests
    ) throws Exception {
        if (guests < 1) {
            throw new Exception("INVALID_REQUEST");
        }

        if (checkOut.compareTo(checkIn) <= 0) {
            throw new Exception("INVALID_REQUEST");
        }
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
