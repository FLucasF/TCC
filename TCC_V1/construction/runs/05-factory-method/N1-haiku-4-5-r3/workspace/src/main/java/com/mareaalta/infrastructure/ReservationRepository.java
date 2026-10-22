package com.mareaalta.infrastructure;

import com.mareaalta.domain.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationRepository {
    public static Reservation save(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                company_cnpj, company_name, guest_passport, guest_country, check_in,
                check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id;
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reservation.getRoomNumber());
            pstmt.setString(2, reservation.getGuestName());
            pstmt.setString(3, reservation.getGuestEmail());
            pstmt.setString(4, reservation.getGuestCpf());
            pstmt.setString(5, reservation.getCompanyCnpj());
            pstmt.setString(6, reservation.getCompanyName());
            pstmt.setString(7, reservation.getGuestPassport());
            pstmt.setString(8, reservation.getGuestCountry());
            pstmt.setDate(9, java.sql.Date.valueOf(reservation.getCheckIn()));
            pstmt.setDate(10, java.sql.Date.valueOf(reservation.getCheckOut()));
            pstmt.setInt(11, reservation.getGuests());
            pstmt.setString(12, reservation.getRatePlan().name());
            pstmt.setInt(13, reservation.getNights());
            pstmt.setBigDecimal(14, reservation.getSubtotal());
            pstmt.setBigDecimal(15, reservation.getDiscount());
            pstmt.setBigDecimal(16, reservation.getTotal());
            pstmt.setString(17, reservation.getStatus().name());
            pstmt.setBigDecimal(18, reservation.getAmountPaid());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                reservation.setId(rs.getLong("id"));
            }
        }
        return reservation;
    }

    public static Reservation findById(long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToReservation(rs);
            }
        }
        return null;
    }

    public static void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getStatus().name());
            pstmt.setBigDecimal(2, reservation.getAmountPaid());
            pstmt.setBigDecimal(3, reservation.getRefund());
            pstmt.setLong(4, reservation.getId());
            pstmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in < ? AND check_out > ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, java.sql.Date.valueOf(checkOut));
            pstmt.setDate(3, java.sql.Date.valueOf(checkIn));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        }
        return true;
    }

    public static List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY check_in
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, java.sql.Date.valueOf(date));
            pstmt.setDate(3, java.sql.Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthStarting(int year, int month) throws SQLException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            AND check_in >= ? AND check_in < ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(start));
            pstmt.setDate(2, java.sql.Date.valueOf(end));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    private static Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.setId(rs.getLong("id"));
        res.setRoomNumber(rs.getInt("room_number"));
        res.setGuestName(rs.getString("guest_name"));
        res.setGuestEmail(rs.getString("guest_email"));
        res.setGuestCpf(rs.getString("guest_cpf"));
        res.setCompanyCnpj(rs.getString("company_cnpj"));
        res.setCompanyName(rs.getString("company_name"));
        res.setGuestPassport(rs.getString("guest_passport"));
        res.setGuestCountry(rs.getString("guest_country"));
        res.setCheckIn(rs.getDate("check_in").toLocalDate());
        res.setCheckOut(rs.getDate("check_out").toLocalDate());
        res.setGuests(rs.getInt("guests"));
        res.setRatePlan(Reservation.RatePlan.valueOf(rs.getString("rate_plan")));
        res.setNights(rs.getInt("nights"));
        res.setSubtotal(rs.getBigDecimal("subtotal"));
        res.setDiscount(rs.getBigDecimal("discount"));
        res.setTotal(rs.getBigDecimal("total"));
        res.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        res.setAmountPaid(rs.getBigDecimal("amount_paid"));
        res.setRefund(rs.getBigDecimal("refund"));
        return res;
    }
}
