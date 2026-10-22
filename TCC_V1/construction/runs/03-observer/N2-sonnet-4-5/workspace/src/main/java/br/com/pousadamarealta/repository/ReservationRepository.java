package br.com.pousadamarealta.repository;

import br.com.pousadamarealta.domain.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public ReservationRepository(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
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
                                     check_in, check_out, guests, rate_plan, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getRoomType().name());
            stmt.setString(3, reservation.getGuestName());
            stmt.setString(4, reservation.getGuestEmail());
            stmt.setString(5, reservation.getGuestCpf());
            stmt.setDate(6, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(7, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(8, reservation.getGuests());
            stmt.setString(9, reservation.getRatePlan().name());
            stmt.setString(10, reservation.getStatus().name());
            stmt.setBigDecimal(11, reservation.getAmountPaid());
            stmt.setBigDecimal(12, reservation.getRefund());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(buildReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservation", e);
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

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            if (excludeId == null) {
                stmt.setNull(4, Types.BIGINT);
                stmt.setNull(5, Types.BIGINT);
            } else {
                stmt.setLong(4, excludeId);
                stmt.setLong(5, excludeId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking conflicts", e);
        }

        return false;
    }

    public List<Reservation> findByDate(LocalDate date) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ? AND check_out > ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservations by date", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservations by month", e);
        }

        return reservations;
    }

    private Reservation buildReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan"))
        );

        reservation.setId(rs.getLong("id"));

        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
        if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT) {
            reservation.confirm();
            if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT) {
                reservation.checkIn();
                if (status == ReservationStatus.CHECKED_OUT) {
                    reservation.checkOut();
                }
            }
        } else if (status == ReservationStatus.CANCELLED) {
            if (rs.getBigDecimal("amount_paid").compareTo(rs.getBigDecimal("refund")) > 0
                || rs.getBigDecimal("refund").signum() > 0) {
                reservation.confirm();
            }
            reservation.cancel();
        }

        return reservation;
    }
}
