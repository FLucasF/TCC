package com.pousadamarealta.repository;

import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

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
                                     check_in, check_out, guests, rate_plan, status,
                                     amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setString(9, reservation.getStatus().name());
            stmt.setBigDecimal(10, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(11, reservation.getRefund());
            } else {
                stmt.setNull(11, Types.DECIMAL);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                reservation.setId(rs.getLong(1));
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
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setString(9, reservation.getStatus().name());
            stmt.setBigDecimal(10, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(11, reservation.getRefund());
            } else {
                stmt.setNull(11, Types.DECIMAL);
            }

            stmt.setLong(12, reservation.getId());

            stmt.executeUpdate();
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();

            while (rs.next()) {
                reservations.add(mapRow(rs));
            }

            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by room and date range", e);
        }
    }

    public List<Reservation> findByDate(LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();

            while (rs.next()) {
                reservations.add(mapRow(rs));
            }

            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by date", e);
        }
    }

    public List<Reservation> findByMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();

            while (rs.next()) {
                reservations.add(mapRow(rs));
            }

            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
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
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        if (rs.getBigDecimal("refund") != null) {
            reservation.setRefund(rs.getBigDecimal("refund"));
        }

        return reservation;
    }
}
