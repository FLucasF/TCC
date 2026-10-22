package com.pousadamarealta.repository;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
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

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

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
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
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

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

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

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check conflicts", e);
        }
        return false;
    }

    public List<Reservation> findByDateAndStatus(LocalDate date, List<ReservationStatus> statuses) {
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toList());
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ? AND check_out > ?
              AND status IN (%s)
            ORDER BY room_number
            """.formatted(placeholders);

        List<Reservation> reservations = new ArrayList<>();
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(3 + i, statuses.get(i).name());
            }

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by date", e);
        }
        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }
        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestCpf(rs.getString("guest_cpf"));
        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        r.setNights(rs.getInt("nights"));
        r.setSubtotal(rs.getBigDecimal("subtotal"));
        r.setDiscount(rs.getBigDecimal("discount"));
        r.setTotal(rs.getBigDecimal("total"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));
        var refund = rs.getBigDecimal("refund");
        if (refund != null) {
            r.setRefund(refund);
        }
        return r;
    }
}
