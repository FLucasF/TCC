package com.pousada.db;

import com.pousada.models.Reservation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Database {
    private final HikariDataSource dataSource;
    private long nextReservationId = 1;

    public Database(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        initializeDatabase();
        loadMaxReservationId();
    }

    private void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGINT PRIMARY KEY,
                room_number INT NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INT NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                nights INT NOT NULL,
                subtotal DECIMAL(10,2) NOT NULL,
                discount DECIMAL(10,2) NOT NULL,
                total DECIMAL(10,2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
                refund DECIMAL(10,2),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reservations table", e);
        }
    }

    private void loadMaxReservationId() {
        String sql = "SELECT MAX(id) as max_id FROM reservations";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long maxId = rs.getLong("max_id");
                if (maxId > 0) {
                    nextReservationId = maxId + 1;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load max reservation ID", e);
        }
    }

    public Reservation saveReservation(Reservation reservation) {
        long id = nextReservationId++;
        String sql = """
            INSERT INTO reservations (id, room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setInt(2, reservation.getRoomNumber());
            stmt.setString(3, reservation.getGuestName());
            stmt.setString(4, reservation.getGuestEmail());
            stmt.setString(5, reservation.getGuestCpf());
            stmt.setDate(6, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(7, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(8, reservation.getGuests());
            stmt.setString(9, reservation.getRatePlan().name());
            stmt.setInt(10, reservation.getNights());
            stmt.setBigDecimal(11, reservation.getSubtotal());
            stmt.setBigDecimal(12, reservation.getDiscount());
            stmt.setBigDecimal(13, reservation.getTotal());
            stmt.setString(14, reservation.getStatus().name());
            stmt.setBigDecimal(15, reservation.getAmountPaid());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }

        return getReservation(id);
    }

    public Reservation updateReservation(Reservation reservation) {
        String sql = """
            UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        return getReservation(reservation.getId());
    }

    public Reservation getReservation(long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parseReservation(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get reservation", e);
        }

        return null;
    }

    public boolean isRoomOccupied(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }

        return false;
    }

    public Map<Integer, Reservation> getReservationsForNight(LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT'
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number ASC
            """;

        Map<Integer, Reservation> results = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = parseReservation(rs);
                    results.put(reservation.getRoomNumber(), reservation);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get occupancy", e);
        }

        return results;
    }

    public java.util.List<Reservation> getReservationsForMonth(LocalDate monthStart, LocalDate monthEnd) {
        String sql = """
            SELECT * FROM reservations
            WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR status = 'CANCELLED')
            AND check_in >= ? AND check_in < ?
            ORDER BY check_in ASC, id ASC
            """;

        java.util.List<Reservation> results = new java.util.ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(monthStart));
            stmt.setDate(2, Date.valueOf(monthEnd));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = parseReservation(rs);
                    if (reservation.getAmountPaid().signum() > 0) {
                        results.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get revenue", e);
        }

        return results;
    }

    private Reservation parseReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            Reservation.RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            Reservation.Status.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }

    public void close() {
        dataSource.close();
    }
}
