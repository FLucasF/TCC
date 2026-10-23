package com.pousada.marealta.repository;

import com.pousada.marealta.config.Database;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

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
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
             guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting reservation", e);
        }

        return reservation;
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                nights = ?, subtotal = ?, discount = ?, total = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(16, reservation.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating reservation", e);
        }

        return reservation;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setString(1, r.getRoomNumber());
        stmt.setString(2, r.getGuestName());
        stmt.setString(3, r.getGuestEmail());
        stmt.setString(4, r.getGuestCpf());
        stmt.setDate(5, Date.valueOf(r.getCheckIn()));
        stmt.setDate(6, Date.valueOf(r.getCheckOut()));
        stmt.setInt(7, r.getGuests());
        stmt.setString(8, r.getRatePlan().name());
        stmt.setInt(9, r.getNights());
        stmt.setBigDecimal(10, r.getSubtotal());
        stmt.setBigDecimal(11, r.getDiscount());
        stmt.setBigDecimal(12, r.getTotal());
        stmt.setString(13, r.getStatus().name());
        stmt.setBigDecimal(14, r.getAmountPaid());
        if (r.getRefund() != null) {
            stmt.setBigDecimal(15, r.getRefund());
        } else {
            stmt.setNull(15, Types.DECIMAL);
        }
    }

    public Reservation findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservation", e);
        }

        return null;
    }

    public List<Reservation> findConflictingReservations(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            if (excludeId != null) {
                stmt.setLong(4, excludeId);
                stmt.setLong(5, excludeId);
            } else {
                stmt.setNull(4, Types.BIGINT);
                stmt.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking room availability", e);
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
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
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ?
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
            throw new RuntimeException("Error fetching reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getString("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));
        reservation.setGuestCpf(rs.getString("guest_cpf"));
        reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
        reservation.setGuests(rs.getInt("guests"));
        reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        reservation.setNights(rs.getInt("nights"));
        reservation.setSubtotal(rs.getBigDecimal("subtotal"));
        reservation.setDiscount(rs.getBigDecimal("discount"));
        reservation.setTotal(rs.getBigDecimal("total"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        if (!rs.wasNull()) {
            reservation.setRefund(refund);
        }

        return reservation;
    }
}
