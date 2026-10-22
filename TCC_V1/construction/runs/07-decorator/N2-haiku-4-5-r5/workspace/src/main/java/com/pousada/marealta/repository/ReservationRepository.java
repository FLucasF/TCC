package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConnection;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public long create(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total
    ) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, nights,
                subtotal, discount, total, status, amount_paid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setDate(5, Date.valueOf(checkIn));
            pstmt.setDate(6, Date.valueOf(checkOut));
            pstmt.setInt(7, guests);
            pstmt.setString(8, ratePlan.getValue());
            pstmt.setInt(9, nights);
            pstmt.setBigDecimal(10, subtotal);
            pstmt.setBigDecimal(11, discount);
            pstmt.setBigDecimal(12, total);
            pstmt.setString(13, ReservationStatus.PENDING.name());
            pstmt.setBigDecimal(14, BigDecimal.ZERO);

            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new Exception("Failed to create reservation");
    }

    public Reservation getById(long id) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToReservation(rs);
            }
        }
        return null;
    }

    public void updateStatus(long id, ReservationStatus status) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        }
    }

    public void updateAmountPaid(long id, BigDecimal amountPaid) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "UPDATE reservations SET amount_paid = ?, status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amountPaid);
            pstmt.setString(2, ReservationStatus.CONFIRMED.name());
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        }
    }

    public void updateRefund(long id, BigDecimal refund) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "UPDATE reservations SET refund = ?, status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, refund);
            pstmt.setString(2, ReservationStatus.CANCELLED.name());
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        }
    }

    public List<Reservation> getReservationsByRoomAndDate(int roomNumber, LocalDate date) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND check_in <= ?
            AND check_out > ?
            AND status != 'CANCELLED'
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setDate(3, Date.valueOf(date));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    public List<Reservation> getReservationsByMonthAndStatus(int year, int month) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            AND amount_paid > 0
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    public List<Reservation> getConfirmedReservationsByDate(LocalDate date) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = """
            SELECT * FROM reservations
            WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setDate(2, Date.valueOf(date));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws Exception {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.fromString(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.fromString(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
