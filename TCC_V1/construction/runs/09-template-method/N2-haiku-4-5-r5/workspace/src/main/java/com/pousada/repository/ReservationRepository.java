package com.pousada.repository;

import com.pousada.database.Database;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.RatePlan;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.sql.Date;

public class ReservationRepository {
    public static Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                    LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                    int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        String sql = "INSERT INTO reservations " +
                "(room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, " +
                "status, nights, subtotal, discount, total, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.name());
            stmt.setString(9, ReservationStatus.PENDING.name());
            stmt.setInt(10, nights);
            stmt.setBigDecimal(11, subtotal);
            stmt.setBigDecimal(12, discount);
            stmt.setBigDecimal(13, total);
            stmt.setBigDecimal(14, BigDecimal.ZERO);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create reservation", e);
        }

        throw new RuntimeException("Failed to create reservation: no ID returned");
    }

    public static Reservation findById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservation", e);
        }

        return null;
    }

    public static List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = "SELECT * FROM reservations WHERE room_number = ? " +
                "AND NOT status = 'CANCELLED' AND NOT (check_out <= ? OR check_in >= ?) " +
                "ORDER BY check_in";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkIn));
            stmt.setDate(3, Date.valueOf(checkOut));

            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
    }

    public static List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) {
        String sql = "SELECT * FROM reservations WHERE room_number = ? " +
                "AND status NOT IN ('CANCELLED') AND check_in <= ? AND check_out > ? " +
                "ORDER BY check_in";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
    }

    public static List<Reservation> findByMonthAndStatuses(int year, int month, String... statuses) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM reservations WHERE EXTRACT(YEAR FROM check_in) = ? " +
                "AND EXTRACT(MONTH FROM check_in) = ? " +
                "AND status IN (");

        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }

        sql.append(") ORDER BY check_in, id");

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);

            for (int i = 0; i < statuses.length; i++) {
                stmt.setString(3 + i, statuses[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
    }

    public static void updateStatus(int id, ReservationStatus status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update reservation status", e);
        }
    }

    public static void updateAmountPaid(int id, BigDecimal amountPaid) {
        String sql = "UPDATE reservations SET amount_paid = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, amountPaid);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update amount paid", e);
        }
    }

    public static void updateRefund(int id, BigDecimal refund) {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update refund", e);
        }
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
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
