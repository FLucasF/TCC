package com.marealata.dao;

import com.marealata.database.Database;
import com.marealata.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationDao {
    public static Reservation save(Reservation reservation) throws SQLException {
        if (reservation.id == null) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private static Reservation insert(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
             rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql,
             Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.roomNumber);
            stmt.setString(2, reservation.guestName);
            stmt.setString(3, reservation.guestEmail);
            stmt.setString(4, reservation.guestCpf);
            stmt.setDate(5, java.sql.Date.valueOf(reservation.checkIn));
            stmt.setDate(6, java.sql.Date.valueOf(reservation.checkOut));
            stmt.setInt(7, reservation.guests);
            stmt.setString(8, reservation.ratePlan.toString());
            stmt.setInt(9, reservation.nights);
            stmt.setBigDecimal(10, reservation.subtotal);
            stmt.setBigDecimal(11, reservation.discount);
            stmt.setBigDecimal(12, reservation.total);
            stmt.setString(13, reservation.status.toString());
            stmt.setBigDecimal(14, reservation.amountPaid);
            stmt.setNull(15, Types.NUMERIC);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.id = rs.getLong(1);
                }
            }
        }
        return reservation;
    }

    private static void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, reservation.status.toString());
            stmt.setBigDecimal(2, reservation.amountPaid);
            if (reservation.refund != null) {
                stmt.setBigDecimal(3, reservation.refund);
            } else {
                stmt.setNull(3, Types.NUMERIC);
            }
            stmt.setLong(4, reservation.id);
            stmt.executeUpdate();
        }
    }

    public static Reservation findById(long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public static List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
                AND check_in < ? AND check_out > ?
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findForOccupancyReport(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findForRevenueReport(LocalDate monthStart, LocalDate monthEnd)
            throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in >= ? AND check_in < ?
            ORDER BY check_in, id
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(monthStart));
            stmt.setDate(2, java.sql.Date.valueOf(monthEnd));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.id = rs.getLong("id");
        res.roomNumber = rs.getInt("room_number");
        res.guestName = rs.getString("guest_name");
        res.guestEmail = rs.getString("guest_email");
        res.guestCpf = rs.getString("guest_cpf");
        res.checkIn = rs.getDate("check_in").toLocalDate();
        res.checkOut = rs.getDate("check_out").toLocalDate();
        res.guests = rs.getInt("guests");
        res.ratePlan = RatePlan.valueOf(rs.getString("rate_plan"));
        res.nights = rs.getInt("nights");
        res.subtotal = rs.getBigDecimal("subtotal");
        res.discount = rs.getBigDecimal("discount");
        res.total = rs.getBigDecimal("total");
        res.status = ReservationStatus.valueOf(rs.getString("status"));
        res.amountPaid = rs.getBigDecimal("amount_paid");
        res.refund = rs.getBigDecimal("refund");
        return res;
    }
}
