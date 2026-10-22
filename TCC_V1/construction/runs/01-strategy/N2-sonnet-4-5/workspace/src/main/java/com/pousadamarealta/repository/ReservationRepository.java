package com.pousadamarealta.repository;

import com.pousadamarealta.domain.RatePlanType;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.ReservationNotFoundException;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
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
            INSERT INTO reservations (room_number, room_type, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan_type, nights, subtotal, discount,
                total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParams(stmt, reservation);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating reservation", e);
        }
    }

    private void setReservationParams(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getRoomType().name());
        stmt.setString(3, reservation.getGuestName());
        stmt.setString(4, reservation.getGuestEmail());
        stmt.setString(5, reservation.getGuestCpf());
        stmt.setDate(6, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(7, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(8, reservation.getGuests());
        stmt.setString(9, reservation.getRatePlanType().name());
        stmt.setLong(10, reservation.getNights());
        stmt.setBigDecimal(11, reservation.getSubtotal());
        stmt.setBigDecimal(12, reservation.getDiscount());
        stmt.setBigDecimal(13, reservation.getTotal());
        stmt.setString(14, reservation.getStatus().name());
        stmt.setBigDecimal(15, reservation.getAmountPaid());
        stmt.setBigDecimal(16, reservation.getRefund());
    }

    public Reservation findById(Long id) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan_type, nights, subtotal, discount, total, status,
                   amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
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

        throw new ReservationNotFoundException("Reservation not found: " + id);
    }

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan_type, nights, subtotal, discount, total, status,
                   amount_paid, refund
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
        } catch (SQLException e) {
            throw new RuntimeException("Error checking room availability", e);
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan_type, nights, subtotal, discount, total, status,
                   amount_paid, refund
            FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
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
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan_type, nights, subtotal, discount, total, status,
                   amount_paid, refund
            FROM reservations
            WHERE check_in >= ?
              AND check_in < ?
              AND (amount_paid > 0 OR refund > 0)
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            LocalDate start = month.atDay(1);
            LocalDate end = month.plusMonths(1).atDay(1);

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
        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlanType.valueOf(rs.getString("rate_plan_type")),
            rs.getLong("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
