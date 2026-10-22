package com.pousadamarealta.repository;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, nights,
                                     subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
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
                stmt.setNull(15, Types.NUMERIC);
            }

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }

        return reservation;
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, Types.NUMERIC);
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
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findConflictingReservations(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conflicting reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate date) {
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

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find paid reservations for night", e);
        }

        return reservations;
    }

    public List<Reservation> findReservationsStartingInMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in <= ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations for month", e);
        }

        return reservations;
    }

    private Reservation mapResultSet(ResultSet rs) throws SQLException {
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
        reservation.setRefund(rs.getBigDecimal("refund"));
        return reservation;
    }
}
