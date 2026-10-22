package com.pousada.marealta.infrastructure.persistence;

import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation save(Reservation reservation) throws SQLException {
        if (reservation.getId() == 0) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, java.math.BigDecimal.valueOf(reservation.getSubtotal()));
            stmt.setBigDecimal(11, java.math.BigDecimal.valueOf(reservation.getDiscount()));
            stmt.setBigDecimal(12, java.math.BigDecimal.valueOf(reservation.getTotal()));
            stmt.setString(13, reservation.getStatus().name());
            stmt.setBigDecimal(14, java.math.BigDecimal.valueOf(reservation.getAmountPaid()));
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(15, java.math.BigDecimal.valueOf(reservation.getRefund()));
            } else {
                stmt.setNull(15, Types.NUMERIC);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return findById(id);
                }
            }
        }
        throw new SQLException("Failed to insert reservation");
    }

    private Reservation update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, java.math.BigDecimal.valueOf(reservation.getAmountPaid()));
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, java.math.BigDecimal.valueOf(reservation.getRefund()));
            } else {
                stmt.setNull(3, Types.NUMERIC);
            }
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        }
        return findById(reservation.getId());
    }

    public Reservation findById(long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findOccupancyByDate(LocalDate date) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findRevenueByMonth(int year, int month) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE (status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'))
            AND EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        int roomNumber = rs.getInt("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        RatePlan ratePlan = RatePlan.fromString(rs.getString("rate_plan"));
        int nights = rs.getInt("nights");
        double subtotal = rs.getBigDecimal("subtotal").doubleValue();
        double discount = rs.getBigDecimal("discount").doubleValue();
        double total = rs.getBigDecimal("total").doubleValue();
        ReservationStatus status = ReservationStatus.fromString(rs.getString("status"));
        double amountPaid = rs.getBigDecimal("amount_paid").doubleValue();
        Double refund = null;
        if (rs.getBigDecimal("refund") != null) {
            refund = rs.getBigDecimal("refund").doubleValue();
        }

        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                status, amountPaid, refund);
    }
}
