package com.pousadamarealta.repository;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Reservation save(Reservation reservation) {
        String sql = """
                INSERT INTO reservations
                (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                 rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
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
            stmt.setBigDecimal(15, reservation.getRefund());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
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
                UPDATE reservations
                SET status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
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
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by room and date range", e);
        }

        return reservations;
    }

    public List<Reservation> findPaidByDate(LocalDate date) {
        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                  AND check_in <= ?
                  AND check_out > ?
                ORDER BY room_number
                """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find paid reservations by date", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth().plusDays(1);

        String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE check_in >= ?
                  AND check_in < ?
                  AND (amount_paid - COALESCE(refund, 0)) > 0
                ORDER BY check_in, id
                """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startOfMonth));
            stmt.setDate(2, Date.valueOf(endOfMonth));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getString("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund")
        );
    }
}
