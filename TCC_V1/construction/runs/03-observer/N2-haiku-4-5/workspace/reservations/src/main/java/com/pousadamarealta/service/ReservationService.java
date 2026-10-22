package com.pousadamarealta.service;

import com.pousadamarealta.database.ReservationDAO;
import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.Money;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationEventDispatcher;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private static final ReservationService INSTANCE = new ReservationService();

    private ReservationService() {
    }

    public static ReservationService getInstance() {
        return INSTANCE;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, String ratePlanCode) throws SQLException {
        Room room = RoomDAO.getInstance().getRoomByNumber(roomNumber);
        if (room == null) {
            throw new RuntimeException("Room not found: " + roomNumber);
        }

        RatePlan ratePlan = RatePlan.fromCode(ratePlanCode);
        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, room.getNightlyRate());

        return ReservationDAO.getInstance().createReservation(reservation);
    }

    public Reservation confirmReservation(long reservationId, Money amountPaid) throws Exception {
        Reservation reservation = ReservationDAO.getInstance().getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("Reservation not found: " + reservationId);
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Cannot confirm a non-pending reservation");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        ReservationDAO.getInstance().updateReservation(reservation);

        GuestNotificationService.sendConfirmationEmail(reservation);
        ReservationEventDispatcher.getInstance().fireOnConfirmed(reservation);

        return reservation;
    }

    public Reservation checkIn(long reservationId) throws SQLException {
        Reservation reservation = ReservationDAO.getInstance().getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("Reservation not found: " + reservationId);
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Cannot check in a non-confirmed reservation");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        ReservationDAO.getInstance().updateReservation(reservation);
        return reservation;
    }

    public Reservation checkOut(long reservationId) throws SQLException {
        Reservation reservation = ReservationDAO.getInstance().getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("Reservation not found: " + reservationId);
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("Cannot check out a non-checked-in reservation");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        ReservationDAO.getInstance().updateReservation(reservation);
        return reservation;
    }

    public Reservation cancelReservation(long reservationId) throws SQLException {
        Reservation reservation = ReservationDAO.getInstance().getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("Reservation not found: " + reservationId);
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel this reservation");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        Money refund = calculateRefund(reservation);
        reservation.setRefund(refund);
        reservation.setStatus(ReservationStatus.CANCELLED);
        ReservationDAO.getInstance().updateReservation(reservation);

        if (wasConfirmed) {
            ReservationEventDispatcher.getInstance().fireOnCancelled(reservation);
        }

        return reservation;
    }

    private Money calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            return Money.zero();
        }
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            return Money.zero();
        }
        return reservation.getAmountPaid();
    }

    public Reservation getReservation(long id) throws SQLException {
        return ReservationDAO.getInstance().getReservationById(id);
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        List<Reservation> reservations = ReservationDAO.getInstance()
                .getReservationsByRoom(roomNumber, checkIn, checkOut);
        return reservations.isEmpty();
    }
}
