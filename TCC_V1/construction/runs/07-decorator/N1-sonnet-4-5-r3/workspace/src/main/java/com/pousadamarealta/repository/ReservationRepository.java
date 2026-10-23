package com.pousadamarealta.repository;

import com.pousadamarealta.domain.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, room_type, base_nightly_rate,
                guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                long id = rs.getLong(1);
                reservation.setId(id);
                return reservation;
            }

            throw new RuntimeException("Failed to get generated ID");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, room_type = ?, base_nightly_rate = ?,
                guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(14, reservation.getId());
            stmt.executeUpdate();

            return reservation;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setString(1, r.getRoomNumber());
        stmt.setString(2, r.getRoomType().name());
        stmt.setBigDecimal(3, r.getRoomType().getNightlyRate());
        stmt.setString(4, r.getGuestName());
        stmt.setString(5, r.getGuestEmail());
        stmt.setString(6, r.getGuestCpf());
        stmt.setDate(7, Date.valueOf(r.getCheckIn()));
        stmt.setDate(8, Date.valueOf(r.getCheckOut()));
        stmt.setInt(9, r.getGuests());
        stmt.setString(10, r.getRatePlan().name());
        stmt.setString(11, r.getStatus().name());
        stmt.setBigDecimal(12, r.getAmountPaid());
        if (r.getRefund() != null) {
            stmt.setBigDecimal(13, r.getRefund());
        } else {
            stmt.setNull(13, Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, room_type, base_nightly_rate,
                   guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   status, amount_paid, refund
            FROM reservations
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(buildReservation(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }
    }

    public boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
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

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) == 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, room_type, base_nightly_rate,
                   guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));
            ResultSet rs = stmt.executeQuery();

            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(buildReservation(rs));
            }

            return reservations;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find occupancy", e);
        }
    }

    public List<Reservation> findRevenueForMonth(int year, int month) {
        String sql = """
            SELECT id, room_number, room_type, base_nightly_rate,
                   guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND (amount_paid - COALESCE(refund, 0)) > 0
            ORDER BY check_in, id
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);
            ResultSet rs = stmt.executeQuery();

            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(buildReservation(rs));
            }

            return reservations;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find revenue", e);
        }
    }

    private Reservation buildReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getBigDecimal("base_nightly_rate"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
