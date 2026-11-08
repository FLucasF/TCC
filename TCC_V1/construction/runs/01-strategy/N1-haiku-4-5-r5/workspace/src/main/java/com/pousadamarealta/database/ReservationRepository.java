package com.pousadamarealta.database;

import com.pousadamarealta.models.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public static Reservation create(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            String ratePlan,
            String companyCode,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total
    ) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0) RETURNING id",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan);
            stmt.setString(9, companyCode);
            stmt.setInt(10, nights);
            stmt.setBigDecimal(11, subtotal);
            stmt.setBigDecimal(12, discount);
            stmt.setBigDecimal(13, total);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id);
                }
            }
        }
        return null;
    }

    public static Reservation findById(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund " +
                "FROM reservations WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public static void updateStatus(int id, String status) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateConfirmation(int id, BigDecimal amountPaid) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ?")) {
            stmt.setBigDecimal(1, amountPaid);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateCancellation(int id, BigDecimal refund) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?")) {
            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public static List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund " +
                "FROM reservations WHERE room_number = ? AND status != 'CANCELLED' AND check_in < ? AND check_out > ? " +
                "ORDER BY check_in")) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthAndPaid(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = month == 12 ? LocalDate.of(year + 1, 1, 1) : LocalDate.of(year, month + 1, 1);

        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund " +
                "FROM reservations WHERE check_in >= ? AND check_in < ? AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR (status = 'CANCELLED' AND amount_paid > 0)) " +
                "ORDER BY check_in, id")) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByDateAndPaid(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund " +
                "FROM reservations WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ? " +
                "ORDER BY room_number")) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapRow(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(
            rs.getInt("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getString("company_code"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getBigDecimal("amount_paid"),
            refund
        );
    }
}
