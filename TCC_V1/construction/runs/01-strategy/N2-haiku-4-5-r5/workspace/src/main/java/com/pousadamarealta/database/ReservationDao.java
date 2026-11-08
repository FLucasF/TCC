package com.pousadamarealta.database;

import com.pousadamarealta.domain.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationDao {
    public static long createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlanType ratePlan,
                                         String companyCode, int nights, BigDecimal subtotal, BigDecimal discount,
                                         BigDecimal total) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                          guests, rate_plan, company_code, nights, subtotal, discount, total, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, java.sql.Date.valueOf(checkIn));
            stmt.setDate(6, java.sql.Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.toString());
            stmt.setString(9, companyCode);
            stmt.setInt(10, nights);
            stmt.setBigDecimal(11, subtotal);
            stmt.setBigDecimal(12, discount);
            stmt.setBigDecimal(13, total);
            stmt.setString(14, ReservationStatus.PENDING.toString());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Failed to create reservation");
    }

    public static Reservation getReservationById(long id) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                       rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapToReservation(rs);
                }
            }
        }

        return null;
    }

    public static void updateReservationStatus(long id, ReservationStatus status) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, status.toString());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateReservationAmountPaid(long id, BigDecimal amountPaid) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET amount_paid = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, amountPaid);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateReservationRefund(long id, BigDecimal refund) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET refund = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, refund);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static List<Reservation> getReservationsByRoomAndDateRange(int roomNumber, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                       rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ? AND check_in < ? AND check_out > ? AND status != 'CANCELLED'
                ORDER BY check_in
                """)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            stmt.setDate(3, java.sql.Date.valueOf(startDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }

        return reservations;
    }

    public static List<Reservation> getConfirmedReservationsByRoomAndNight(int roomNumber, LocalDate night) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                       rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ? AND check_in <= ? AND check_out > ?
                      AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                """)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(night));
            stmt.setDate(3, java.sql.Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }

        return reservations;
    }

    public static List<Reservation> getReservationsStartingInMonth(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                       rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE check_in >= ? AND check_in <= ?
                      AND (amount_paid > 0 OR status IN ('CHECKED_IN', 'CHECKED_OUT'))
                ORDER BY check_in, id
                """)) {

            stmt.setDate(1, java.sql.Date.valueOf(firstDay));
            stmt.setDate(2, java.sql.Date.valueOf(lastDay));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }

        return reservations;
    }

    private static Reservation mapToReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlanType.valueOf(rs.getString("rate_plan")),
            rs.getString("company_code"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total")
        );
        res.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        res.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        if (refund != null) {
            res.setRefund(refund);
        }

        return res;
    }
}
