package com.pousadamarealta.repository;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParams(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Reservation(
                        id,
                        reservation.getRoomNumber(),
                        reservation.getGuestName(),
                        reservation.getGuestEmail(),
                        reservation.getGuestCpf(),
                        reservation.getCheckIn(),
                        reservation.getCheckOut(),
                        reservation.getGuests(),
                        reservation.getRatePlan(),
                        reservation.getNights(),
                        reservation.getSubtotal(),
                        reservation.getDiscount(),
                        reservation.getTotal(),
                        reservation.getStatus(),
                        reservation.getAmountPaid(),
                        reservation.getRefund()
                    );
                }
                throw new RuntimeException("Failed to get generated ID");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
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
    }

    private void setReservationParams(PreparedStatement stmt, Reservation reservation) throws SQLException {
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
        stmt.setBigDecimal(15, reservation.getRefund());
    }

    public Optional<Reservation> findById(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }
    }

    public boolean hasConflictingReservation(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            stmt.setObject(4, excludeId);
            stmt.setObject(5, excludeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check conflicts", e);
        }
    }

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE check_in <= ? AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by night", e);
        }
    }

    public List<Reservation> findByCheckInMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT * FROM reservations
            WHERE check_in >= ? AND check_in < ?
            ORDER BY check_in, id
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
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
            rs.getObject("refund") != null ? rs.getBigDecimal("refund") : null
        );
    }
}
