package com.pousadamarealta.database;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDao {
    private final Database database;

    public ReservationDao(Database database) {
        this.database = database;
    }

    public Reservation create(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, reservation.getSubtotal());
            stmt.setBigDecimal(11, reservation.getDiscount());
            stmt.setBigDecimal(12, reservation.getTotal());
            stmt.setString(13, reservation.getStatus().name());
            stmt.setBigDecimal(14, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(15, reservation.getRefund());
            } else {
                stmt.setNull(15, Types.DECIMAL);
            }

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getLong(1));
                }
            }

            return reservation;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reservation", e);
        }
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?,
                amount_paid = ?,
                refund = ?
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }

            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            if (excludeReservationId != null) {
                stmt.setLong(4, excludeReservationId);
                stmt.setLong(5, excludeReservationId);
            } else {
                stmt.setNull(4, Types.BIGINT);
                stmt.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }

        return false;
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations for night", e);
        }

        return reservations;
    }

    public List<Reservation> findReservationsStartingInMonth(YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate startOfNextMonth = month.plusMonths(1).atDay(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ?
              AND check_in < ?
              AND (amount_paid - COALESCE(refund, 0)) > 0
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startOfMonth));
            stmt.setDate(2, Date.valueOf(startOfNextMonth));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations for month", e);
        }

        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
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
