package com.posadamarealta.repository;

import com.posadamarealta.model.Reservation;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DataSource ds;

    public ReservationRepository(DataSource ds) {
        this.ds = ds;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail,
                             String guestCpf, String companyCnpj, String companyName,
                             String guestPassport, String guestCountry,
                             LocalDate checkIn, LocalDate checkOut, int guests,
                             String ratePlan, int nights, BigDecimal subtotal,
                             BigDecimal discount, BigDecimal total) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "company_cnpj, company_name, guest_passport, guest_country, check_in, check_out, " +
                "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setString(5, companyCnpj);
            stmt.setString(6, companyName);
            stmt.setString(7, guestPassport);
            stmt.setString(8, guestCountry);
            stmt.setDate(9, java.sql.Date.valueOf(checkIn));
            stmt.setDate(10, java.sql.Date.valueOf(checkOut));
            stmt.setInt(11, guests);
            stmt.setString(12, ratePlan);
            stmt.setInt(13, nights);
            stmt.setBigDecimal(14, subtotal);
            stmt.setBigDecimal(15, discount);
            stmt.setBigDecimal(16, total);
            stmt.setString(17, "PENDING");
            stmt.setBigDecimal(18, BigDecimal.ZERO);

            stmt.executeUpdate();

            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                        companyCnpj, companyName, guestPassport, guestCountry,
                        checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                        "PENDING", BigDecimal.ZERO, null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to create reservation");
    }

    public Optional<Reservation> findById(int id) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public void updateStatus(int id, String status) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void confirm(int id, BigDecimal amountPaid) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?")) {
            stmt.setString(1, "CONFIRMED");
            stmt.setBigDecimal(2, amountPaid);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel(int id, BigDecimal refund) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, refund = ? WHERE id = ?")) {
            stmt.setString(1, "CANCELLED");
            stmt.setBigDecimal(2, refund);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT * FROM reservations WHERE room_number = ? " +
                "AND check_in <= ? AND check_out > ? " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "ORDER BY check_in")) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            stmt.setDate(3, java.sql.Date.valueOf(date));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reservations;
    }

    public List<Reservation> findByMonthAndStarting(YearMonth yearMonth) {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT * FROM reservations WHERE check_in >= ? AND check_in <= ? " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                "ORDER BY check_in, id")) {
            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
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
