package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public static Reservation save(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                     "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.roomNumber);
            stmt.setString(2, reservation.guestName);
            stmt.setString(3, reservation.guestEmail);
            stmt.setString(4, reservation.guestCpf);
            stmt.setDate(5, java.sql.Date.valueOf(reservation.checkIn));
            stmt.setDate(6, java.sql.Date.valueOf(reservation.checkOut));
            stmt.setInt(7, reservation.guests);
            stmt.setString(8, reservation.ratePlan);
            stmt.setInt(9, reservation.nights);
            stmt.setBigDecimal(10, reservation.subtotal);
            stmt.setBigDecimal(11, reservation.discount);
            stmt.setBigDecimal(12, reservation.total);
            stmt.setString(13, reservation.status);
            stmt.setBigDecimal(14, reservation.amountPaid);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.id = rs.getLong(1);
                }
            }
        }
        return reservation;
    }

    public static Reservation findById(long id) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                     "FROM reservations WHERE id = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public static void update(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            stmt.setString(1, reservation.status);
            stmt.setBigDecimal(2, reservation.amountPaid);
            stmt.setObject(3, reservation.refund);
            stmt.setLong(4, reservation.id);
            stmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM reservations " +
                     "WHERE room_number = ? AND status != 'CANCELLED' " +
                     "AND check_in < ? AND check_out > ?")) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        }
    }

    public static List<Reservation> findByRoomAndMonth(int roomNumber, int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                     "FROM reservations " +
                     "WHERE room_number = ? AND EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
                     "AND status != 'CANCELLED' " +
                     "ORDER BY check_in")) {
            stmt.setInt(1, roomNumber);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByNightForOccupancy(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate nextDay = date.plusDays(1);
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                     "FROM reservations " +
                     "WHERE check_in <= ? AND check_out > ? AND status != 'CANCELLED' AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT') " +
                     "ORDER BY room_number")) {
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

    public static List<Reservation> findByMonthForRevenue(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                     "FROM reservations " +
                     "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
                     "AND (amount_paid > 0 OR refund IS NOT NULL) " +
                     "ORDER BY check_in, id")) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.id = rs.getLong("id");
        r.roomNumber = rs.getInt("room_number");
        r.guestName = rs.getString("guest_name");
        r.guestEmail = rs.getString("guest_email");
        r.guestCpf = rs.getString("guest_cpf");
        r.checkIn = rs.getDate("check_in").toLocalDate();
        r.checkOut = rs.getDate("check_out").toLocalDate();
        r.guests = rs.getInt("guests");
        r.ratePlan = rs.getString("rate_plan");
        r.nights = rs.getInt("nights");
        r.subtotal = rs.getBigDecimal("subtotal");
        r.discount = rs.getBigDecimal("discount");
        r.total = rs.getBigDecimal("total");
        r.status = rs.getString("status");
        r.amountPaid = rs.getBigDecimal("amount_paid");
        r.refund = rs.getBigDecimal("refund");
        return r;
    }
}
