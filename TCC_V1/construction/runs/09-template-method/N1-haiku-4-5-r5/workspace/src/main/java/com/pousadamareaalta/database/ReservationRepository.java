package com.pousadamareaalta.database;

import com.pousadamareaalta.domain.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationRepository {
    private static final String INSERT_SQL = """
        INSERT INTO reservations
        (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    public static long create(Reservation reservation) throws SQLException {
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(INSERT_SQL)) {
            stmt.setInt(1, reservation.roomNumber());
            stmt.setString(2, reservation.guestName());
            stmt.setString(3, reservation.guestEmail());
            stmt.setString(4, reservation.guestCpf());
            stmt.setObject(5, reservation.checkIn());
            stmt.setObject(6, reservation.checkOut());
            stmt.setInt(7, reservation.guests());
            stmt.setString(8, reservation.ratePlan());
            stmt.setInt(9, reservation.nights());
            stmt.setBigDecimal(10, new java.math.BigDecimal(String.format("%.2f", reservation.subtotal())));
            stmt.setBigDecimal(11, new java.math.BigDecimal(String.format("%.2f", reservation.discount())));
            stmt.setBigDecimal(12, new java.math.BigDecimal(String.format("%.2f", reservation.total())));
            stmt.setString(13, reservation.status());
            stmt.setBigDecimal(14, new java.math.BigDecimal("0.00"));
            stmt.setObject(15, null);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public static Optional<Reservation> findById(long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public static void updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateAmountPaid(long id, double amountPaid) throws SQLException {
        String sql = "UPDATE reservations SET amount_paid = ? WHERE id = ?";
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setBigDecimal(1, new java.math.BigDecimal(String.format("%.2f", amountPaid)));
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateRefund(long id, Double refund) throws SQLException {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            if (refund != null) {
                stmt.setBigDecimal(1, new java.math.BigDecimal(String.format("%.2f", refund)));
            } else {
                stmt.setNull(1, Types.NUMERIC);
            }
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, checkOut);
            stmt.setObject(3, checkIn);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        }
    }

    public static List<Reservation> findByMonthStarting(int year, int month) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByDateOccupancy(LocalDate date) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setObject(1, date);
            stmt.setObject(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        Double refund = rs.getObject("refund") != null ? rs.getDouble("refund") : null;
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getObject("check_in", LocalDate.class),
            rs.getObject("check_out", LocalDate.class),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getInt("nights"),
            rs.getDouble("subtotal"),
            rs.getDouble("discount"),
            rs.getDouble("total"),
            rs.getString("status"),
            rs.getDouble("amount_paid"),
            refund
        );
    }
}
