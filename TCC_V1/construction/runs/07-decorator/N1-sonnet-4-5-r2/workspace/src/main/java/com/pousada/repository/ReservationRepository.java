package com.pousada.repository;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.infrastructure.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, status,
                                     subtotal, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong("id"));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, subtotal = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getSubtotal());
            stmt.setBigDecimal(3, reservation.getAmountPaid());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(4, reservation.getRefund());
            } else {
                stmt.setNull(4, Types.DECIMAL);
            }
            stmt.setLong(5, reservation.getId());

            stmt.executeUpdate();
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());
        stmt.setString(4, reservation.getGuestCpf());
        stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(7, reservation.getGuests());
        stmt.setString(8, reservation.getRatePlan().name());
        stmt.setString(9, reservation.getStatus().name());
        stmt.setBigDecimal(10, reservation.getSubtotal());
        stmt.setBigDecimal(11, reservation.getAmountPaid());
        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(12, reservation.getRefund());
        } else {
            stmt.setNull(12, Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status,
                   subtotal, amount_paid, refund
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

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status,
                   subtotal, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check conflicts", e);
        }

        return reservations;
    }

    public List<Reservation> findByNightAndPaid(LocalDate night) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status,
                   subtotal, amount_paid, refund
            FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonthAndRevenue(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status,
                   subtotal, amount_paid, refund
            FROM reservations
            WHERE check_in >= ?
              AND check_in < ?
              AND (amount_paid > 0 OR refund < amount_paid + COALESCE(refund, 0))
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan"))
        );

        reservation.setId(rs.getLong("id"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setSubtotal(rs.getBigDecimal("subtotal"));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        if (!rs.wasNull()) {
            reservation.setRefund(refund);
        }

        return reservation;
    }
}
