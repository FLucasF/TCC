package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Reservation.RatePlan;
import com.pousada.marealta.domain.Reservation.ReservationStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    public Reservation save(Reservation res) throws SQLException {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
             rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, res.getRoomNumber());
            stmt.setString(2, res.getGuestName());
            stmt.setString(3, res.getGuestEmail());
            stmt.setString(4, res.getGuestCpf());
            stmt.setObject(5, res.getCheckIn());
            stmt.setObject(6, res.getCheckOut());
            stmt.setInt(7, res.getGuests());
            stmt.setString(8, res.getRatePlan().name());
            stmt.setInt(9, res.getNights());
            stmt.setBigDecimal(10, res.getSubtotal());
            stmt.setBigDecimal(11, res.getDiscount());
            stmt.setBigDecimal(12, res.getTotal());
            stmt.setString(13, res.getStatus().name());
            stmt.setBigDecimal(14, res.getAmountPaid());
            stmt.setObject(15, res.getRefund());

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return findById(rs.getLong(1));
                }
            }
        }

        return null;
    }

    public Reservation findById(long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return buildReservation(rs);
                }
            }
        }

        return null;
    }

    public void update(Reservation res) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, res.getStatus().name());
            stmt.setBigDecimal(2, res.getAmountPaid());
            stmt.setObject(3, res.getRefund());
            stmt.setLong(4, res.getId());
            stmt.executeUpdate();
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status NOT IN ('CANCELLED')
            AND check_in < ?
            AND check_out > ?
            """;

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, checkOut);
            stmt.setObject(3, checkIn);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }

        return false;
    }

    public List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, date);
            stmt.setObject(3, date);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) throws SQLException {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in <= ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            ORDER BY check_in ASC, id ASC
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, startOfMonth);
            stmt.setObject(2, endOfMonth);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation buildReservation(java.sql.ResultSet rs) throws SQLException {
        return new Reservation.Builder()
            .id(rs.getLong("id"))
            .roomNumber(rs.getInt("room_number"))
            .guestName(rs.getString("guest_name"))
            .guestEmail(rs.getString("guest_email"))
            .guestCpf(rs.getString("guest_cpf"))
            .checkIn(rs.getObject("check_in", LocalDate.class))
            .checkOut(rs.getObject("check_out", LocalDate.class))
            .guests(rs.getInt("guests"))
            .ratePlan(RatePlan.valueOf(rs.getString("rate_plan")))
            .nights(rs.getInt("nights"))
            .subtotal(rs.getBigDecimal("subtotal"))
            .discount(rs.getBigDecimal("discount"))
            .total(rs.getBigDecimal("total"))
            .status(ReservationStatus.valueOf(rs.getString("status")))
            .amountPaid(rs.getBigDecimal("amount_paid"))
            .refund(rs.getObject("refund", java.math.BigDecimal.class))
            .build();
    }
}
