package com.pousadamareaalta.repositories;

import com.pousadamareaalta.Database;
import com.pousadamareaalta.models.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public long create(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reservation.roomNumber);
            stmt.setString(2, reservation.guestName);
            stmt.setString(3, reservation.guestEmail);
            stmt.setString(4, reservation.guestCpf);
            stmt.setString(5, reservation.companyCnpj);
            stmt.setString(6, reservation.companyName);
            stmt.setString(7, reservation.guestPassport);
            stmt.setString(8, reservation.guestCountry);
            stmt.setDate(9, java.sql.Date.valueOf(reservation.checkIn));
            stmt.setDate(10, java.sql.Date.valueOf(reservation.checkOut));
            stmt.setInt(11, reservation.guests);
            stmt.setString(12, reservation.ratePlan);
            stmt.setInt(13, reservation.nights);
            stmt.setBigDecimal(14, new java.math.BigDecimal(reservation.subtotal));
            stmt.setBigDecimal(15, new java.math.BigDecimal(reservation.discount));
            stmt.setBigDecimal(16, new java.math.BigDecimal(reservation.total));
            stmt.setString(17, reservation.status);
            stmt.setBigDecimal(18, new java.math.BigDecimal(reservation.amountPaid));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new RuntimeException("Failed to create reservation");
    }

    public Reservation findById(long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }

        return null;
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations SET
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.status);
            stmt.setBigDecimal(2, new java.math.BigDecimal(reservation.amountPaid));

            if (reservation.refund != null) {
                stmt.setBigDecimal(3, new java.math.BigDecimal(reservation.refund));
            } else {
                stmt.setNull(3, Types.NUMERIC);
            }

            stmt.setLong(4, reservation.id);
            stmt.executeUpdate();
        }
    }

    public List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ? AND check_in <= ? AND check_out > ? AND status != 'CANCELLED'
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            stmt.setDate(3, java.sql.Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ? AND check_in < ? AND check_out > ? AND status != 'CANCELLED'
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonthWithMoney(int year, int month) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
            AND amount_paid - COALESCE(refund, 0) > 0
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation mapResultSet(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.id = rs.getLong("id");
        r.roomNumber = rs.getInt("room_number");
        r.guestName = rs.getString("guest_name");
        r.guestEmail = rs.getString("guest_email");
        r.guestCpf = rs.getString("guest_cpf");
        r.companyCnpj = rs.getString("company_cnpj");
        r.companyName = rs.getString("company_name");
        r.guestPassport = rs.getString("guest_passport");
        r.guestCountry = rs.getString("guest_country");
        r.checkIn = rs.getDate("check_in").toLocalDate();
        r.checkOut = rs.getDate("check_out").toLocalDate();
        r.guests = rs.getInt("guests");
        r.ratePlan = rs.getString("rate_plan");
        r.nights = rs.getInt("nights");
        r.subtotal = rs.getBigDecimal("subtotal").doubleValue();
        r.discount = rs.getBigDecimal("discount").doubleValue();
        r.total = rs.getBigDecimal("total").doubleValue();
        r.status = rs.getString("status");
        r.amountPaid = rs.getBigDecimal("amount_paid").doubleValue();

        java.math.BigDecimal refundBD = rs.getBigDecimal("refund");
        r.refund = refundBD != null ? refundBD.doubleValue() : null;

        return r;
    }
}
