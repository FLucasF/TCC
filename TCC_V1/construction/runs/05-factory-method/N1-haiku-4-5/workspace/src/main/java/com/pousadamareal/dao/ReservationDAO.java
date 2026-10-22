package com.pousadamareal.dao;

import com.pousadamareal.config.DatabaseConfig;
import com.pousadamareal.model.Reservation;
import com.pousadamareal.model.ReservationStatus;
import com.pousadamareal.model.RatePlan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    public static Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                               String guestCpf, String companyCnpj, String companyName,
                                               String guestPassport, String guestCountry,
                                               String checkIn, String checkOut, int guests,
                                               String ratePlan, int nights, double subtotal,
                                               double discount, double total) throws Exception {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                                     company_name, guest_passport, guest_country, check_in, check_out,
                                     guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(roomNumber));
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setString(5, companyCnpj);
            pstmt.setString(6, companyName);
            pstmt.setString(7, guestPassport);
            pstmt.setString(8, guestCountry);
            pstmt.setString(9, checkIn);
            pstmt.setString(10, checkOut);
            pstmt.setInt(11, guests);
            pstmt.setString(12, ratePlan);
            pstmt.setInt(13, nights);
            pstmt.setDouble(14, subtotal);
            pstmt.setDouble(15, discount);
            pstmt.setDouble(16, total);
            pstmt.setString(17, "PENDING");
            pstmt.setDouble(18, 0);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return getReservationById(rs.getInt("id"));
                }
            }
        }

        return null;
    }

    public static Reservation getReservationById(int id) throws Exception {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }

        return null;
    }

    public static void updateReservationStatus(int id, String status, double amountPaid, Double refund) throws Exception {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setDouble(2, amountPaid);
            if (refund != null) {
                pstmt.setDouble(3, refund);
            } else {
                pstmt.setNull(3, Types.DOUBLE);
            }
            pstmt.setInt(4, id);

            pstmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailableForDates(int roomNumber, String checkIn, String checkOut) throws Exception {
        String sql = """
            SELECT COUNT(*) as count FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, checkOut);
            pstmt.setString(3, checkIn);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") == 0;
                }
            }
        }

        return false;
    }

    public static List<Reservation> getReservationsByDateAndStatus(String date, String status) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT * FROM reservations
            WHERE status = ? AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setString(2, date);
            pstmt.setString(3, date);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }

        return reservations;
    }

    public static List<Reservation> getReservationsByMonth(String month) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT * FROM reservations
            WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR (status = 'CANCELLED' AND rate_plan = 'NON_REFUNDABLE'))
            AND DATE_TRUNC('month', check_in::timestamp) = ?::date
            ORDER BY check_in, id
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, month + "-01");

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
            rs.getInt("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getString("company_cnpj"),
            rs.getString("company_name"),
            rs.getString("guest_passport"),
            rs.getString("guest_country"),
            rs.getDate("check_in").toString(),
            rs.getDate("check_out").toString(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getDouble("subtotal"),
            rs.getDouble("discount"),
            rs.getDouble("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getDouble("amount_paid"),
            rs.getObject("refund") != null ? rs.getDouble("refund") : null
        );
    }
}
