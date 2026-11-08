package com.marealta.db;

import com.marealta.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRepository.class);

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, reservation.getRoomNumber());
            pstmt.setString(2, reservation.getGuestName());
            pstmt.setString(3, reservation.getGuestEmail());
            pstmt.setString(4, reservation.getGuestCpf());
            pstmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            pstmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            pstmt.setInt(7, reservation.getGuests());
            pstmt.setString(8, reservation.getRatePlan().toString());
            pstmt.setInt(9, reservation.getNights());
            pstmt.setBigDecimal(10, reservation.getSubtotal());
            pstmt.setBigDecimal(11, reservation.getDiscount());
            pstmt.setBigDecimal(12, reservation.getTotal());
            pstmt.setString(13, reservation.getStatus().toString());
            pstmt.setBigDecimal(14, reservation.getAmountPaid());
            pstmt.setObject(15, reservation.getRefund());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving reservation", e);
            throw new RuntimeException(e);
        }

        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reservation by id", e);
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(checkOut));
            pstmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations by room and date range", e);
            throw new RuntimeException(e);
        }

        return reservations;
    }

    public List<Reservation> findConfirmedOrPaidByRoomAndDate(int roomNumber, LocalDate date) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setDate(3, Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding confirmed or paid reservations", e);
            throw new RuntimeException(e);
        }

        return reservations;
    }

    public List<Reservation> findByMonthAndStatuses(LocalDate monthStart, LocalDate monthEnd, List<String> statuses) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ? AND status IN (
            """);

        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(") ORDER BY check_in, id");

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setDate(1, Date.valueOf(monthStart));
            pstmt.setDate(2, Date.valueOf(monthEnd));
            for (int i = 0; i < statuses.size(); i++) {
                pstmt.setString(3 + i, statuses.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations by month and statuses", e);
            throw new RuntimeException(e);
        }

        return reservations;
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getStatus().toString());
            pstmt.setBigDecimal(2, reservation.getAmountPaid());
            pstmt.setObject(3, reservation.getRefund());
            pstmt.setLong(4, reservation.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating reservation", e);
            throw new RuntimeException(e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            Reservation.RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            Reservation.Status.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
