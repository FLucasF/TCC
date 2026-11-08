package pousada.repository;

import pousada.config.Database;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, reservation.getRoomNumber());
            pstmt.setString(2, reservation.getGuestName());
            pstmt.setString(3, reservation.getGuestEmail());
            pstmt.setString(4, reservation.getGuestCpf());
            pstmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            pstmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            pstmt.setInt(7, reservation.getGuests());
            pstmt.setString(8, reservation.getRatePlan().name());
            pstmt.setInt(9, reservation.getNights());
            pstmt.setBigDecimal(10, reservation.getSubtotal());
            pstmt.setBigDecimal(11, reservation.getDiscount());
            pstmt.setBigDecimal(12, reservation.getTotal());
            pstmt.setString(13, reservation.getStatus().name());
            pstmt.setBigDecimal(14, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                pstmt.setBigDecimal(15, reservation.getRefund());
            } else {
                pstmt.setNull(15, Types.DECIMAL);
            }

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
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
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reservation.getStatus().name());
            pstmt.setBigDecimal(2, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                pstmt.setBigDecimal(3, reservation.getRefund());
            } else {
                pstmt.setNull(3, Types.DECIMAL);
            }

            pstmt.setLong(4, reservation.getId());
            pstmt.executeUpdate();
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
            FROM reservations WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            AND (? IS NULL OR id != ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(checkOut));
            pstmt.setDate(3, Date.valueOf(checkIn));

            if (excludeId != null) {
                pstmt.setLong(4, excludeId);
                pstmt.setLong(5, excludeId);
            } else {
                pstmt.setNull(4, Types.BIGINT);
                pstmt.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check reservation conflict", e);
        }
    }

    public List<Reservation> findPaidReservationsForDate(LocalDate date) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        return findByDateQuery(sql, date, date);
    }

    public List<Reservation> findRevenueReservationsForMonth(YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ?
            AND (amount_paid - COALESCE(refund, 0)) > 0
            ORDER BY check_in, id
        """;

        return findByDateQuery(sql, startOfMonth, endOfMonth);
    }

    public List<Reservation> findPaidReservationsStartingInMonth(YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY check_in, id
        """;

        return findByDateQuery(sql, startOfMonth, endOfMonth);
    }

    private List<Reservation> findByDateQuery(String sql, LocalDate param1, LocalDate param2) {
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(param1));
            pstmt.setDate(2, Date.valueOf(param2));

            try (ResultSet rs = pstmt.executeQuery()) {
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

        if (rs.getBigDecimal("refund") != null) {
            reservation.setRefund(rs.getBigDecimal("refund"));
        }

        return reservation;
    }
}
