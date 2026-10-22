package com.pousada.marealta.repository;

import com.pousada.marealta.database.Database;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.ReservationStatus;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public static Reservation save(Reservation res) throws SQLException {
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                    company_cnpj, company_name, guest_passport, guest_country,
                    check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status,
                    amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, res.getRoomNumber());
            stmt.setString(2, res.getGuestName());
            stmt.setString(3, res.getGuestEmail());
            stmt.setString(4, res.getGuestCpf());
            stmt.setString(5, res.getCompanyCnpj());
            stmt.setString(6, res.getCompanyName());
            stmt.setString(7, res.getGuestPassport());
            stmt.setString(8, res.getGuestCountry());
            stmt.setDate(9, Date.valueOf(res.getCheckIn()));
            stmt.setDate(10, Date.valueOf(res.getCheckOut()));
            stmt.setInt(11, res.getGuests());
            stmt.setString(12, res.getRatePlan().toString());
            stmt.setLong(13, res.getNights());
            stmt.setBigDecimal(14, res.getSubtotal());
            stmt.setBigDecimal(15, res.getDiscount());
            stmt.setBigDecimal(16, res.getTotal());
            stmt.setString(17, res.getStatus().toString());
            stmt.setBigDecimal(18, res.getAmountPaid());
            stmt.setNull(19, java.sql.Types.NUMERIC);

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    res.setId(rs.getLong(1));
                }
            }
        }
        return res;
    }

    public static Optional<Reservation> findById(long id) throws SQLException {
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                       company_name, guest_passport, guest_country, check_in, check_out, guests,
                       rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """)) {
            stmt.setLong(1, id);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    public static void update(Reservation res) throws SQLException {
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("""
                UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?
                """)) {
            stmt.setString(1, res.getStatus().toString());
            stmt.setBigDecimal(2, res.getAmountPaid());
            stmt.setBigDecimal(3, res.getRefund());
            stmt.setLong(4, res.getId());
            stmt.executeUpdate();
        }
    }

    public static boolean hasConflict(int roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId)
            throws SQLException {
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("""
                SELECT COUNT(*) FROM reservations
                WHERE room_number = ?
                  AND status != 'CANCELLED'
                  AND check_in < ?
                  AND check_out > ?
                  AND (? IS NULL OR id != ?)
                """)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            stmt.setObject(4, excludeId);
            stmt.setObject(5, excludeId);

            var rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public static List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                       company_name, guest_passport, guest_country, check_in, check_out, guests,
                       rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ?
                  AND check_in <= ?
                  AND check_out > ?
                  AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                ORDER BY room_number
                """)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonth(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("""
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                       company_name, guest_passport, guest_country, check_in, check_out, guests,
                       rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE check_in >= ? AND check_in <= ?
                  AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
                  AND (amount_paid > 0 OR refund IS NOT NULL)
                ORDER BY check_in, id
                """)) {
            stmt.setDate(1, Date.valueOf(firstDay));
            stmt.setDate(2, Date.valueOf(lastDay));

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        }
        return reservations;
    }

    private static Reservation mapRow(ResultSet rs) throws SQLException {
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
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getLong("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
