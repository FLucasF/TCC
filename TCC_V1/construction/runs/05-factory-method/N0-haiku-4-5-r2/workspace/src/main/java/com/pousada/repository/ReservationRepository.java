package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.model.Reservation;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail,
                             String guestCpf, String companyCnpj, String companyName,
                             String guestPassport, String guestCountry,
                             LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                             int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0)
            RETURNING id
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setString(5, companyCnpj);
            stmt.setString(6, companyName);
            stmt.setString(7, guestPassport);
            stmt.setString(8, guestCountry);
            stmt.setDate(9, Date.valueOf(checkIn));
            stmt.setDate(10, Date.valueOf(checkOut));
            stmt.setInt(11, guests);
            stmt.setString(12, ratePlan);
            stmt.setInt(13, nights);
            stmt.setBigDecimal(14, subtotal);
            stmt.setBigDecimal(15, discount);
            stmt.setBigDecimal(16, total);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            long id = rs.getLong(1);

            return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
                    guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
                    subtotal, discount, total, "PENDING", BigDecimal.ZERO, null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Reservation> findById(long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateStatus(long id, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAmountPaid(long id, BigDecimal amount) {
        String sql = "UPDATE reservations SET amount_paid = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, amount);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateRefund(long id, BigDecimal refund) {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, refund);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> findConflicting(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> findByDateForOccupancy(LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> findByMonthForRevenue(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            AND check_in >= ? AND check_in < ?
            AND (status != 'CANCELLED' OR amount_paid > 0)
            ORDER BY check_in, id
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getString("company_cnpj"),
            rs.getString("company_name"),
            rs.getString("guest_passport"),
            rs.getString("guest_country"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
