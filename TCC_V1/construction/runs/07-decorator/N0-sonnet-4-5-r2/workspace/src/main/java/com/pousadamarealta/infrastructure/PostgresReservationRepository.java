package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.model.RatePlan;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.ReservationStatus;
import com.pousadamarealta.domain.repository.ReservationRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresReservationRepository implements ReservationRepository {

    private final DataSource dataSource;

    public PostgresReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, nights,
                                     subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setId(keys.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?, nights = ?,
                subtotal = ?, discount = ?, total = ?, status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(16, reservation.getId());
            stmt.executeUpdate();

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
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
        if (r.getRefund() == null) {
            stmt.setNull(15, Types.DECIMAL);
        } else {
            stmt.setBigDecimal(15, r.getRefund());
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }
    }

    @Override
    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conflicting reservations", e);
        }
    }

    @Override
    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations for night", e);
        }
    }

    @Override
    public List<Reservation> findReservationsStartingInMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND (amount_paid > 0 OR refund > 0)
            ORDER BY check_in, id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations for month", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
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
