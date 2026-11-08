package com.pousadamarealta.db;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.ReservationStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    public static long insert(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                 "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ",
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
            if (reservation.refund != null) {
                stmt.setBigDecimal(15, reservation.refund);
            } else {
                stmt.setNull(15, java.sql.Types.NUMERIC);
            }

            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public static Optional<Reservation> findById(long id) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public static void update(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            stmt.setString(1, reservation.status.toString());
            stmt.setBigDecimal(2, reservation.amountPaid);
            if (reservation.refund != null) {
                stmt.setBigDecimal(3, reservation.refund);
            } else {
                stmt.setNull(3, java.sql.Types.NUMERIC);
            }
            stmt.setLong(4, reservation.id);
            stmt.executeUpdate();
        }
    }

    public static List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date, String excludeStatus) throws SQLException {
        List<Reservation> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT * FROM reservations WHERE room_number = ? AND check_in <= ? AND check_out > ? AND status != ?")) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setString(4, excludeStatus);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public static List<Reservation> findConflicting(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        List<Reservation> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT * FROM reservations WHERE room_number = ? AND check_in < ? AND check_out > ? AND status != 'CANCELLED'")) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public static List<Reservation> findByMonthStartDate(LocalDate monthStart) throws SQLException {
        List<Reservation> result = new ArrayList<>();
        LocalDate monthEnd = monthStart.plusMonths(1);
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT * FROM reservations WHERE check_in >= ? AND check_in < ? " +
                 "AND amount_paid > 0 ORDER BY check_in, id")) {
            stmt.setDate(1, java.sql.Date.valueOf(monthStart));
            stmt.setDate(2, java.sql.Date.valueOf(monthEnd));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public static List<Reservation> findByDateOccupancy(LocalDate date) throws SQLException {
        List<Reservation> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT * FROM reservations WHERE check_in <= ? AND check_out > ? " +
                 "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') ORDER BY room_number")) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setDate(2, java.sql.Date.valueOf(date));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private static Reservation mapRow(java.sql.ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
