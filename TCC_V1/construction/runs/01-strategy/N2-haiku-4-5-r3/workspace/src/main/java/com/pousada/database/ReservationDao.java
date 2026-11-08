package com.pousada.database;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlanType;
import com.pousada.util.Dates;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationDao {
    public static Reservation insert(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlanType ratePlan,
                                     String companyCode, int nights, BigDecimal subtotal, BigDecimal discount,
                                     BigDecimal total) throws SQLException {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setObject(5, checkIn);
            pstmt.setObject(6, checkOut);
            pstmt.setInt(7, guests);
            pstmt.setString(8, ratePlan.name());
            pstmt.setString(9, companyCode);
            pstmt.setInt(10, nights);
            pstmt.setBigDecimal(11, subtotal);
            pstmt.setBigDecimal(12, discount);
            pstmt.setBigDecimal(13, total);
            pstmt.setString(14, ReservationStatus.PENDING.name());
            pstmt.setBigDecimal(15, BigDecimal.ZERO);

            ResultSet rs = pstmt.executeQuery();
            rs.next();
            int id = rs.getInt("id");

            return findById(id);
        }
    }

    public static Reservation findById(int id) throws SQLException {
        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                    rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapReservation(rs);
            }
        }
        return null;
    }

    public static void updateStatus(int id, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public static void updateAmountPaid(int id, BigDecimal amountPaid) throws SQLException {
        String sql = "UPDATE reservations SET amount_paid = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amountPaid);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public static void updateRefund(int id, BigDecimal refund) throws SQLException {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, refund);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public static List<Reservation> findByRoomAndNight(int roomNumber, LocalDate night) throws SQLException {
        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                    rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ? AND check_in <= ? AND check_out > ? AND status != 'CANCELLED'
                """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setObject(2, night);
            pstmt.setObject(3, night);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthAndNotCancelled(int year, int month) throws SQLException {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                    rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
                ORDER BY check_in, id
                """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlanType.valueOf(rs.getString("rate_plan")),
                rs.getString("company_code"),
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
