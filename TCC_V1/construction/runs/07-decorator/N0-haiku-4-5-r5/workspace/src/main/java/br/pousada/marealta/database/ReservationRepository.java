package br.pousada.marealta.database;

import br.pousada.marealta.model.Reservation;
import br.pousada.marealta.model.RatePlan;
import br.pousada.marealta.model.ReservationStatus;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    public static Reservation insert(Connection conn, Reservation reservation) throws Exception {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                      check_in, check_out, guests, rate_plan, nights,
                                      subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().toString());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, new java.math.BigDecimal(reservation.getSubtotal()));
            stmt.setBigDecimal(11, new java.math.BigDecimal(reservation.getDiscount()));
            stmt.setBigDecimal(12, new java.math.BigDecimal(reservation.getTotal()));
            stmt.setString(13, reservation.getStatus().toString());
            stmt.setBigDecimal(14, new java.math.BigDecimal(reservation.getAmountPaid()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return findById(conn, id).orElseThrow();
                }
            }
        }
        throw new RuntimeException("Failed to insert reservation");
    }

    public static Optional<Reservation> findById(Connection conn, long id) throws Exception {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount,
                   total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public static void update(Connection conn, Reservation reservation) throws Exception {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().toString());
            stmt.setBigDecimal(2, new java.math.BigDecimal(reservation.getAmountPaid()));
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, new java.math.BigDecimal(reservation.getRefund()));
            } else {
                stmt.setNull(3, Types.NUMERIC);
            }
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(Connection conn, int roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        return false;
    }

    public static List<Reservation> findByMonth(Connection conn, String month) throws Exception {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount,
                   total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            AND to_char(check_in, 'YYYY-MM') = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByDate(Connection conn, LocalDate date) throws Exception {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount,
                   total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Double refund = rs.getDouble("refund");
        if (rs.wasNull()) {
            refund = null;
        }

        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getDouble("subtotal"),
            rs.getDouble("discount"),
            rs.getDouble("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getDouble("amount_paid"),
            refund
        );
    }
}
