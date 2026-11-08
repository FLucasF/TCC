package com.pousadamarealta.repository;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PostgresReservationRepository implements ReservationRepository {
    private final Database database;

    public PostgresReservationRepository(Database database) {
        this.database = database;
    }

    @Override
    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservation.roomNumber());
            stmt.setString(2, reservation.guestName());
            stmt.setString(3, reservation.guestEmail());
            stmt.setString(4, reservation.guestCpf());
            stmt.setDate(5, Date.valueOf(reservation.checkIn()));
            stmt.setDate(6, Date.valueOf(reservation.checkOut()));
            stmt.setInt(7, reservation.guests());
            stmt.setString(8, reservation.ratePlan().name());
            stmt.setInt(9, reservation.nights());
            stmt.setBigDecimal(10, reservation.subtotal());
            stmt.setBigDecimal(11, reservation.discount());
            stmt.setBigDecimal(12, reservation.total());
            stmt.setString(13, reservation.status().name());
            stmt.setBigDecimal(14, reservation.amountPaid());
            if (reservation.refund() != null) {
                stmt.setBigDecimal(15, reservation.refund());
            } else {
                stmt.setNull(15, Types.DECIMAL);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Reservation(
                        id,
                        reservation.roomNumber(),
                        reservation.guestName(),
                        reservation.guestEmail(),
                        reservation.guestCpf(),
                        reservation.checkIn(),
                        reservation.checkOut(),
                        reservation.guests(),
                        reservation.ratePlan(),
                        reservation.nights(),
                        reservation.subtotal(),
                        reservation.discount(),
                        reservation.total(),
                        reservation.status(),
                        reservation.amountPaid(),
                        reservation.refund()
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }

        throw new RuntimeException("Failed to get generated ID");
    }

    @Override
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

            stmt.setString(1, reservation.status().name());
            stmt.setBigDecimal(2, reservation.amountPaid());
            if (reservation.refund() != null) {
                stmt.setBigDecimal(3, reservation.refund());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }
            stmt.setLong(4, reservation.id());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    @Override
    public boolean hasConflictingReservation(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
        """;

        try (Connection conn = database.getConnection();
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
            throw new RuntimeException("Failed to check conflicts", e);
        }

        return false;
    }

    @Override
    public List<Reservation> findByRoomAndDateAndStatuses(int roomNumber, LocalDate date, List<ReservationStatus> statuses) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND check_in <= ?
              AND check_out > ?
              AND status IN (
        """);

        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(4 + i, statuses.get(i).name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByCheckInMonthAndStatuses(YearMonth month, List<ReservationStatus> statuses) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND status IN (
        """);

        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(") ORDER BY check_in, id");

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(3 + i, statuses.get(i).name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
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
