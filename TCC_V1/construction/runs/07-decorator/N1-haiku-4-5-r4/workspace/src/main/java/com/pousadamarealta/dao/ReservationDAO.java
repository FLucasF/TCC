package com.pousadamarealta.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.models.RatePlan;
import com.pousadamarealta.db.Database;

public class ReservationDAO {
    public static Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                     int nights, java.math.BigDecimal subtotal, java.math.BigDecimal discount,
                                     java.math.BigDecimal total) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.name());
            stmt.setInt(9, nights);
            stmt.setBigDecimal(10, subtotal);
            stmt.setBigDecimal(11, discount);
            stmt.setBigDecimal(12, total);
            stmt.setString(13, ReservationStatus.PENDING.name());
            stmt.setBigDecimal(14, java.math.BigDecimal.ZERO);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            int id = rs.getInt("id");

            return getById(id);
        }
    }

    public static Reservation getById(int id) throws SQLException {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                     "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    public static void updateStatus(int id, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateStatusAndAmountPaid(int id, ReservationStatus status, java.math.BigDecimal amountPaid) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setBigDecimal(2, amountPaid);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }

    public static void updateRefund(int id, java.math.BigDecimal refund) throws SQLException {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM reservations " +
                     "WHERE room_number = ? " +
                     "AND status != 'CANCELLED' " +
                     "AND check_in < ? " +
                     "AND check_out > ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") == 0;
        }
    }

    public static List<Reservation> getByRoomAndDate(LocalDate date) throws SQLException {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                     "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                     "FROM reservations " +
                     "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                     "AND check_in <= ? " +
                     "AND check_out > ? " +
                     "ORDER BY room_number";

        List<Reservation> results = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        }
        return results;
    }

    public static List<Reservation> getByMonthAndStatus(int year, int month) throws SQLException {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                     "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                     "FROM reservations " +
                     "WHERE EXTRACT(YEAR FROM check_in) = ? " +
                     "AND EXTRACT(MONTH FROM check_in) = ? " +
                     "AND amount_paid > 0 " +
                     "ORDER BY check_in, id";

        List<Reservation> results = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        }
        return results;
    }

    private static Reservation mapResultSet(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("id"),
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
