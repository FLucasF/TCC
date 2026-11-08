package com.pousada.repository;

import com.pousada.config.DatabaseConfig;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class ReservationRepository {
    public static Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan,
             nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, java.sql.Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, java.sql.Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().getName());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, reservation.getSubtotal());
            stmt.setBigDecimal(11, reservation.getDiscount());
            stmt.setBigDecimal(12, reservation.getTotal());
            stmt.setString(13, reservation.getStatus().name());
            stmt.setBigDecimal(14, reservation.getAmountPaid());
            stmt.setObject(15, reservation.getRefund());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                reservation.setId(rs.getLong("id"));
                return reservation;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
        return null;
    }

    public static Reservation findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapReservation(rs);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservation", e);
        }
        return null;
    }

    public static void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setObject(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT COUNT(*) as count FROM reservations
            WHERE room_number = ?
            AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
            AND check_in < ?
            AND check_out > ?
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check room availability", e);
        }
        return false;
    }

    public static List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status NOT IN ('CANCELLED')
            AND check_in < ?
            AND check_out > ?
            ORDER BY check_in
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
        return reservations;
    }

    public static List<Reservation> findByDateAndStatus(LocalDate date, ReservationStatus status) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT * FROM reservations
            WHERE check_in <= ?
            AND check_out > ?
            AND status = ?
            ORDER BY room_number
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setDate(2, java.sql.Date.valueOf(date));
            stmt.setString(3, status.name());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
        return reservations;
    }

    public static List<Reservation> findByMonthAndStatus(int year, int month, ReservationStatus status) {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        String sql = """
            SELECT * FROM reservations
            WHERE check_in >= ?
            AND check_in < ?
            AND status = ?
            ORDER BY check_in, id
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(monthStart));
            stmt.setDate(2, java.sql.Date.valueOf(monthEnd));
            stmt.setString(3, status.name());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
        return reservations;
    }

    public static List<Reservation> findByMonthForRevenue(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        String sql = """
            SELECT * FROM reservations
            WHERE check_in >= ?
            AND check_in < ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY check_in, id
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(monthStart));
            stmt.setDate(2, java.sql.Date.valueOf(monthEnd));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find reservations", e);
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getInt("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestCpf(rs.getString("guest_cpf"));
        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(RatePlan.fromString(rs.getString("rate_plan")));
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
