package com.pousadamarealta.repository;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.RatePlan;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
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
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                      guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
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

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setId(keys.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving reservation", e);
        }
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                                    check_in = ?, check_out = ?, guests = ?, rate_plan = ?, nights = ?,
                                    subtotal = ?, discount = ?, total = ?, status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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
            stmt.setLong(16, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findConflictingReservations(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
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
            throw new RuntimeException("Error finding conflicting reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findByCheckInMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations by month", e);
        }

        return reservations;
    }

    public List<Reservation> findOccupyingRoom(String roomNumber, LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding occupying reservations", e);
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
        reservation.setRefund(rs.wasNull() ? null : refund);
        return reservation;
    }
}
