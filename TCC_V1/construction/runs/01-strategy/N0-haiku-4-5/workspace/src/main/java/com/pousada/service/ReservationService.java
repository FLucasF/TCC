package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.db.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    public static Reservation createReservation(
            int roomNumber, String guestName, String guestEmail, String guestCpf,
            LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total, int nights) throws SQLException {

        try (Connection conn = Database.getConnection()) {
            String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

            var stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setObject(5, checkIn);
            stmt.setObject(6, checkOut);
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan);
            stmt.setString(9, companyCode);
            stmt.setInt(10, nights);
            stmt.setBigDecimal(11, subtotal);
            stmt.setBigDecimal(12, discount);
            stmt.setBigDecimal(13, total);
            stmt.setString(14, "PENDING");
            stmt.setBigDecimal(15, BigDecimal.ZERO);

            var rs = stmt.executeQuery();
            rs.next();
            int reservationId = rs.getInt("id");

            return getReservationById(reservationId);
        }
    }

    public static Reservation getReservationById(int reservationId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT * FROM reservations WHERE id = ?";
            var stmt = conn.prepareStatement(sql);
            stmt.setInt(1, reservationId);

            var rs = stmt.executeQuery();
            if (!rs.next()) {
                return null;
            }

            return resultSetToReservation(rs);
        }
    }

    public static void updateReservationStatus(int reservationId, String newStatus) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ? WHERE id = ?";
            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, reservationId);
            stmt.executeUpdate();
        }
    }

    public static void confirmReservation(int reservationId, BigDecimal amountPaid) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";
            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, "CONFIRMED");
            stmt.setBigDecimal(2, amountPaid);
            stmt.setInt(3, reservationId);
            stmt.executeUpdate();
        }
    }

    public static void refundReservation(int reservationId, BigDecimal refundAmount) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";
            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, "CANCELLED");
            stmt.setBigDecimal(2, refundAmount);
            stmt.setInt(3, reservationId);
            stmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = """
                SELECT COUNT(*) as count FROM reservations
                WHERE room_number = ? AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
                AND NOT (check_out <= ? OR check_in >= ?)
                """;

            var stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, checkIn);
            stmt.setObject(3, checkOut);

            var rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") == 0;
        }
    }

    public static List<Reservation> getReservationsByDate(LocalDate date) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = """
                SELECT * FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ? AND check_out > ?
                ORDER BY room_number
                """;

            var stmt = conn.prepareStatement(sql);
            stmt.setObject(1, date);
            stmt.setObject(2, date);

            List<Reservation> reservations = new ArrayList<>();
            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(resultSetToReservation(rs));
            }
            return reservations;
        }
    }

    public static List<Reservation> getReservationsByMonth(LocalDate monthStart) throws SQLException {
        LocalDate monthEnd = monthStart.plusMonths(1);

        try (Connection conn = Database.getConnection()) {
            String sql = """
                SELECT * FROM reservations
                WHERE (status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') OR status = 'CANCELLED')
                AND check_in >= ? AND check_in < ?
                AND (amount_paid > 0 OR refund IS NOT NULL)
                ORDER BY check_in, id
                """;

            var stmt = conn.prepareStatement(sql);
            stmt.setObject(1, monthStart);
            stmt.setObject(2, monthEnd);

            List<Reservation> reservations = new ArrayList<>();
            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(resultSetToReservation(rs));
            }
            return reservations;
        }
    }

    private static Reservation resultSetToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getObject("check_in", LocalDate.class),
            rs.getObject("check_out", LocalDate.class),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
