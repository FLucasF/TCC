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

public class ReservationRepository {

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
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, reservation);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                reservation.setId(rs.getLong("id"));
                return reservation;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET room_number = ?, guest_name = ?, guest_email = ?,
                guest_cpf = ?, check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                nights = ?, subtotal = ?, discount = ?, total = ?, status = ?,
                amount_paid = ?, refund = ?
            WHERE id = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, reservation);
            stmt.setLong(16, reservation.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setParameters(PreparedStatement stmt, Reservation r) throws SQLException {
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
        stmt.setBigDecimal(15, r.getRefund());
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate start, LocalDate end) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(end));
            stmt.setDate(3, Date.valueOf(start));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
    }

    public List<Reservation> findPaidByDate(LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
            """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find occupancy", e);
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
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }
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
        r.setRefund(rs.getBigDecimal("refund"));
        return r;
    }
}
