package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Reservation.ReservationStatus;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.notification.NotificationService;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests,
                              Reservation.RatePlan ratePlan) throws SQLException {
        validateDates(checkIn, checkOut);
        validateGuests(guests);

        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException();
        }

        validateCapacity(guests, room.getCapacity());
        validateAvailability(roomNumber, checkIn, checkOut);

        var pricing = PricingService.calculatePrice(room, checkIn, checkOut, ratePlan);

        var reservation = new Reservation.Builder()
            .roomNumber(roomNumber)
            .guestName(guestName)
            .guestEmail(guestEmail)
            .guestCpf(guestCpf)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .guests(guests)
            .ratePlan(ratePlan)
            .nights(pricing.nights)
            .subtotal(pricing.subtotal)
            .discount(pricing.discount)
            .total(pricing.total)
            .status(ReservationStatus.PENDING)
            .amountPaid(java.math.BigDecimal.ZERO)
            .build();

        return reservationRepository.save(reservation);
    }

    public Reservation confirm(long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException("OPERATION_NOT_ALLOWED");
        }

        var updated = new Reservation.Builder()
            .id(reservation.getId())
            .roomNumber(reservation.getRoomNumber())
            .guestName(reservation.getGuestName())
            .guestEmail(reservation.getGuestEmail())
            .guestCpf(reservation.getGuestCpf())
            .checkIn(reservation.getCheckIn())
            .checkOut(reservation.getCheckOut())
            .guests(reservation.getGuests())
            .ratePlan(reservation.getRatePlan())
            .nights(reservation.getNights())
            .subtotal(reservation.getSubtotal())
            .discount(reservation.getDiscount())
            .total(reservation.getTotal())
            .status(ReservationStatus.CONFIRMED)
            .amountPaid(reservation.getTotal())
            .refund(reservation.getRefund())
            .build();

        reservationRepository.update(updated);
        notificationService.notifyReservationConfirmed(updated);

        return updated;
    }

    public Reservation cancel(long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidOperationException("OPERATION_NOT_ALLOWED");
        }

        java.math.BigDecimal refund = java.math.BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            refund = reservation.getRatePlan() == Reservation.RatePlan.STANDARD
                ? reservation.getAmountPaid()
                : java.math.BigDecimal.ZERO;
        }

        var updated = new Reservation.Builder()
            .id(reservation.getId())
            .roomNumber(reservation.getRoomNumber())
            .guestName(reservation.getGuestName())
            .guestEmail(reservation.getGuestEmail())
            .guestCpf(reservation.getGuestCpf())
            .checkIn(reservation.getCheckIn())
            .checkOut(reservation.getCheckOut())
            .guests(reservation.getGuests())
            .ratePlan(reservation.getRatePlan())
            .nights(reservation.getNights())
            .subtotal(reservation.getSubtotal())
            .discount(reservation.getDiscount())
            .total(reservation.getTotal())
            .status(ReservationStatus.CANCELLED)
            .amountPaid(reservation.getAmountPaid())
            .refund(refund.signum() > 0 ? refund : null)
            .build();

        reservationRepository.update(updated);
        notificationService.notifyReservationCancelled(updated);

        return updated;
    }

    public Reservation checkIn(long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidOperationException("OPERATION_NOT_ALLOWED");
        }

        var updated = new Reservation.Builder()
            .id(reservation.getId())
            .roomNumber(reservation.getRoomNumber())
            .guestName(reservation.getGuestName())
            .guestEmail(reservation.getGuestEmail())
            .guestCpf(reservation.getGuestCpf())
            .checkIn(reservation.getCheckIn())
            .checkOut(reservation.getCheckOut())
            .guests(reservation.getGuests())
            .ratePlan(reservation.getRatePlan())
            .nights(reservation.getNights())
            .subtotal(reservation.getSubtotal())
            .discount(reservation.getDiscount())
            .total(reservation.getTotal())
            .status(ReservationStatus.CHECKED_IN)
            .amountPaid(reservation.getAmountPaid())
            .refund(reservation.getRefund())
            .build();

        reservationRepository.update(updated);
        return updated;
    }

    public Reservation checkOut(long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new InvalidOperationException("OPERATION_NOT_ALLOWED");
        }

        var updated = new Reservation.Builder()
            .id(reservation.getId())
            .roomNumber(reservation.getRoomNumber())
            .guestName(reservation.getGuestName())
            .guestEmail(reservation.getGuestEmail())
            .guestCpf(reservation.getGuestCpf())
            .checkIn(reservation.getCheckIn())
            .checkOut(reservation.getCheckOut())
            .guests(reservation.getGuests())
            .ratePlan(reservation.getRatePlan())
            .nights(reservation.getNights())
            .subtotal(reservation.getSubtotal())
            .discount(reservation.getDiscount())
            .total(reservation.getTotal())
            .status(ReservationStatus.CHECKED_OUT)
            .amountPaid(reservation.getAmountPaid())
            .refund(reservation.getRefund())
            .build();

        reservationRepository.update(updated);
        return updated;
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidOperationException("INVALID_REQUEST");
        }
    }

    private void validateGuests(int guests) {
        if (guests < 1) {
            throw new InvalidOperationException("INVALID_REQUEST");
        }
    }

    private void validateCapacity(int guests, int capacity) {
        if (guests > capacity) {
            throw new CapacityExceededException();
        }
    }

    private void validateAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        if (!reservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }
    }

    public static class RoomNotFoundException extends RuntimeException {
    }

    public static class ReservationNotFoundException extends RuntimeException {
    }

    public static class CapacityExceededException extends RuntimeException {
    }

    public static class RoomUnavailableException extends RuntimeException {
    }

    public static class InvalidOperationException extends RuntimeException {
        private final String code;

        public InvalidOperationException(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
