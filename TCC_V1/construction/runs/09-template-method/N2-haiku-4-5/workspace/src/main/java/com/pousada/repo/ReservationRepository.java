package com.pousada.repo;

import com.pousada.domain.Reservation;
import com.pousada.domain.RatePlan;
import com.pousada.domain.ReservationStatus;
import com.pousada.util.Database;

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
    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                              int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) throws Exception {
        Connection conn = Database.getConnection();

        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setDate(5, Date.valueOf(checkIn));
            pstmt.setDate(6, Date.valueOf(checkOut));
            pstmt.setInt(7, guests);
            pstmt.setString(8, ratePlan.name());
            pstmt.setInt(9, nights);
            pstmt.setBigDecimal(10, subtotal);
            pstmt.setBigDecimal(11, discount);
            pstmt.setBigDecimal(12, total);
            pstmt.setString(13, ReservationStatus.PENDING.name());
            pstmt.setBigDecimal(14, BigDecimal.ZERO);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return findById(id);
                }
            }
        }

        return null;
    }

    public Reservation findById(int id) throws Exception {
        Connection conn = Database.getConnection();

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        }

        return null;
    }

    public void updateStatus(int id, ReservationStatus status) throws Exception {
        Connection conn = Database.getConnection();

        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void confirm(int id, BigDecimal amountPaid) throws Exception {
        Connection conn = Database.getConnection();

        String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ReservationStatus.CONFIRMED.name());
            pstmt.setBigDecimal(2, amountPaid);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    public void cancel(int id, BigDecimal refund) throws Exception {
        Connection conn = Database.getConnection();

        String sql = "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ReservationStatus.CANCELLED.name());
            pstmt.setBigDecimal(2, refund);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    public List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut, String excludeStatus) throws Exception {
        Connection conn = Database.getConnection();
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND check_in < ? AND check_out > ? AND status != ?
            ORDER BY check_in
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(checkOut));
            pstmt.setDate(3, Date.valueOf(checkIn));
            pstmt.setString(4, excludeStatus);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonthAndPaid(int year, int month) throws Exception {
        Connection conn = Database.getConnection();
        List<Reservation> reservations = new ArrayList<>();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY check_in, id
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByDateAndPaid(LocalDate date) throws Exception {
        Connection conn = Database.getConnection();
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ? AND check_out > ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws Exception {
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
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            refund != null ? refund : null
        );
    }
}
