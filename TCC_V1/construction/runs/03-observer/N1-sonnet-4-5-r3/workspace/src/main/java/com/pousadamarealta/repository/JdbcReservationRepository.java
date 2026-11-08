package com.pousadamarealta.repository;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    private final DataSource dataSource;

    public JdbcReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
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
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    reservation.setId(id);
                }
            }

            return reservation;

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?,
                amount_paid = ?,
                refund = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
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

            return reservation;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservation by id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Reservation> findByRoomAndPeriod(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations by room and period", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByNightAndStatus(LocalDate night, ReservationStatus... statuses) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
        """);

        if (statuses.length > 0) {
            sql.append(" AND status IN (");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
            }
            sql.append(")");
        }

        sql.append(" ORDER BY room_number");

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            for (int i = 0; i < statuses.length; i++) {
                stmt.setString(3 + i, statuses[i].name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations by night and status", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByMonthAndStatuses(YearMonth month, ReservationStatus... statuses) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ?
              AND check_in < ?
        """);

        if (statuses.length > 0) {
            sql.append(" AND status IN (");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
            }
            sql.append(")");
        }

        sql.append(" ORDER BY check_in, id");

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            LocalDate start = month.atDay(1);
            LocalDate end = month.plusMonths(1).atDay(1);

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            for (int i = 0; i < statuses.length; i++) {
                stmt.setString(3 + i, statuses[i].name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations by month and statuses", e);
        }

        return reservations;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws Exception {
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
    }

    private Reservation mapReservation(ResultSet rs) throws Exception {
        BigDecimal refundValue = rs.getBigDecimal("refund");

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
            rs.wasNull() ? null : refundValue
        );
    }
}
