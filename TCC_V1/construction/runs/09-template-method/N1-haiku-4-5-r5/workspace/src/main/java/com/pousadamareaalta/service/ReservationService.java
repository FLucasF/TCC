package com.pousadamareaalta.service;

import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.util.PriceCalculator;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReservationService {
    public static Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                    LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan)
            throws SQLException {

        Optional<Room> room = RoomRepository.findByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.get().capacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        double subtotal = PriceCalculator.calculateSubtotal(room.get().nightlyRate(), checkIn, checkOut);
        double discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        double total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total,
            "PENDING",
            0.0,
            null
        );

        long id = ReservationRepository.create(reservation);
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
            "PENDING", 0.0, null);
    }

    public static Reservation getById(long id) throws SQLException {
        Optional<Reservation> reservation = ReservationRepository.findById(id);
        if (reservation.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return reservation.get();
    }

    public static Reservation confirm(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (!"PENDING".equals(reservation.status())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatus(id, "CONFIRMED");
        ReservationRepository.updateAmountPaid(id, reservation.total());

        return reservation.withStatus("CONFIRMED").withAmountPaid(reservation.total());
    }

    public static Reservation checkIn(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (!"CONFIRMED".equals(reservation.status())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatus(id, "CHECKED_IN");
        return reservation.withStatus("CHECKED_IN");
    }

    public static Reservation checkOut(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (!"CHECKED_IN".equals(reservation.status())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatus(id, "CHECKED_OUT");
        return reservation.withStatus("CHECKED_OUT");
    }

    public static Reservation cancel(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (!"PENDING".equals(reservation.status()) && !"CONFIRMED".equals(reservation.status())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Double refund = null;
        if ("CONFIRMED".equals(reservation.status())) {
            if ("STANDARD".equals(reservation.ratePlan())) {
                refund = reservation.amountPaid();
            } else {
                refund = 0.0;
            }
        }

        ReservationRepository.updateStatus(id, "CANCELLED");
        if (refund != null) {
            ReservationRepository.updateRefund(id, refund);
        }

        return reservation.withStatus("CANCELLED").withRefund(refund);
    }
}
