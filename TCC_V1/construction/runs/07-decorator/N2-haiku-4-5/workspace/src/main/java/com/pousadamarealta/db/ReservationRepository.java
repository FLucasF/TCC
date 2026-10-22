package com.pousadamarealta.db;

import com.pousadamarealta.model.Reservation;
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

    public Reservation save(Reservation reservation) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {
            setReservationStatement(stmt, reservation);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getLong(1));
                }
            }
        }
        return reservation;
    }

    public void update(Reservation reservation) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                """
                UPDATE reservations SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                    check_in = ?, check_out = ?, guests = ?, rate_plan = ?, nights = ?, subtotal = ?,
                    discount = ?, total = ?, status = ?, amount_paid = ?, refund = ? WHERE id = ?
                """)) {
            setReservationStatement(stmt, reservation);
            stmt.setLong(16, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public Optional<Reservation> findById(Long id) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(Integer roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                """
                SELECT * FROM reservations
                WHERE room_number = ? AND status != 'CANCELLED'
                AND check_in < ? AND check_out > ?
                ORDER BY check_in
                """)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                """
                SELECT * FROM reservations
                WHERE check_in >= ? AND check_in < ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
                ORDER BY check_in, id
                """)) {
            stmt.setDate(1, Date.valueOf(startOfMonth));
            stmt.setDate(2, Date.valueOf(startOfNextMonth));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByDateForOccupancy(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                """
                SELECT * FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ? AND check_out > ?
                ORDER BY room_number
                """)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }
        return reservations;
    }

    private void setReservationStatement(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setInt(1, r.getRoomNumber());
        stmt.setString(2, r.getGuestName());
        stmt.setString(3, r.getGuestEmail());
        stmt.setString(4, r.getGuestCpf());
        stmt.setDate(5, Date.valueOf(r.getCheckIn()));
        stmt.setDate(6, Date.valueOf(r.getCheckOut()));
        stmt.setInt(7, r.getGuests());
        stmt.setString(8, r.getRatePlan());
        stmt.setInt(9, r.getNights());
        stmt.setBigDecimal(10, r.getSubtotal());
        stmt.setBigDecimal(11, r.getDiscount());
        stmt.setBigDecimal(12, r.getTotal());
        stmt.setString(13, r.getStatus());
        stmt.setBigDecimal(14, r.getAmountPaid());
        stmt.setBigDecimal(15, r.getRefund());
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
