package com.pousada.marealta.db;

import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public static Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                     int nights, java.math.BigDecimal subtotal, java.math.BigDecimal discount, java.math.BigDecimal total) throws Exception {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setDate(5, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(6, java.sql.Date.valueOf(checkOut));
            pstmt.setInt(7, guests);
            pstmt.setString(8, ratePlan.name());
            pstmt.setInt(9, nights);
            pstmt.setBigDecimal(10, subtotal);
            pstmt.setBigDecimal(11, discount);
            pstmt.setBigDecimal(12, total);
            pstmt.setString(13, ReservationStatus.PENDING.name());
            pstmt.setBigDecimal(14, java.math.BigDecimal.ZERO);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                            guests, ratePlan, nights, subtotal, discount, total, ReservationStatus.PENDING,
                            java.math.BigDecimal.ZERO, null);
                }
            }
        }
        return null;
    }

    public static Reservation findById(long id) throws Exception {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public static void updateStatus(long id, ReservationStatus status) throws Exception {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        }
    }

    public static void updateStatusAndAmountPaid(long id, ReservationStatus status, java.math.BigDecimal amountPaid) throws Exception {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setBigDecimal(2, amountPaid);
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        }
    }

    public static void updateStatusAndRefund(long id, ReservationStatus status, java.math.BigDecimal refund) throws Exception {
        String sql = "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setBigDecimal(2, refund);
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        String sql = "SELECT COUNT(*) FROM reservations WHERE room_number = ? AND status != 'CANCELLED' " +
                "AND check_in < ? AND check_out > ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, java.sql.Date.valueOf(checkOut));
            pstmt.setDate(3, java.sql.Date.valueOf(checkIn));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        return true;
    }

    public static List<Reservation> findByDateAndStatus(LocalDate date, ReservationStatus status) throws Exception {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations " +
                "WHERE check_in <= ? AND check_out > ? AND status = ? ORDER BY room_number, id";

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            pstmt.setDate(2, java.sql.Date.valueOf(date));
            pstmt.setString(3, status.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthAndStatus(int year, int month, ReservationStatus status) throws Exception {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations " +
                "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
                "AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR status = 'CANCELLED') " +
                "ORDER BY check_in, id";

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
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
