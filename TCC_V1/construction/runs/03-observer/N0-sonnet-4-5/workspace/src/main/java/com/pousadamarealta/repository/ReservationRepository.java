package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
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
                INSERT INTO reservations
                (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                 guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        return reservation;
    }

    private Reservation update(Reservation reservation) {
        String sql = """
                UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                nights = ?, subtotal = ?, discount = ?, total = ?,
                status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(16, reservation.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        return reservation;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
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

        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(15, reservation.getRefund());
        } else {
            stmt.setNull(15, Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
                SELECT * FROM reservations
                WHERE room_number = ?
                AND status != 'CANCELLED'
                AND check_in < ?
                AND check_out > ?
                AND (? IS NULL OR id != ?)
                """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
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
            throw new RuntimeException("Failed to check room availability", e);
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
                SELECT * FROM reservations
                WHERE check_in <= ?
                AND check_out > ?
                AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND amount_paid > 0
                ORDER BY room_number
                """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch occupancy", e);
        }

        return reservations;
    }

    public List<Reservation> findByCheckInMonth(int year, int month) {
        String sql = """
                SELECT * FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ?
                AND EXTRACT(MONTH FROM check_in) = ?
                AND (amount_paid - COALESCE(refund, 0)) > 0
                ORDER BY check_in, id
                """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch revenue report", e);
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

        if (rs.getBigDecimal("refund") != null) {
            reservation.setRefund(rs.getBigDecimal("refund"));
        }

        return reservation;
    }
}
