package com.pousada.db;

import com.pousada.models.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                             int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) throws SQLException {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
             rate_plan, nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, java.sql.Date.valueOf(checkIn));
            stmt.setDate(6, java.sql.Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.getLabel());
            stmt.setInt(9, nights);
            stmt.setBigDecimal(10, subtotal);
            stmt.setBigDecimal(11, discount);
            stmt.setBigDecimal(12, total);
            stmt.setString(13, ReservationStatus.PENDING.getLabel());
            stmt.setBigDecimal(14, BigDecimal.ZERO);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                            ReservationStatus.PENDING, BigDecimal.ZERO, null);
                }
            }
        }

        throw new SQLException("Failed to create reservation");
    }

    public Optional<Reservation> findById(long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToReservation(rs));
                }
            }
        }

        return Optional.empty();
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().getLabel());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
            AND status != ?
            AND NOT (check_out <= ? OR check_in >= ?)
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setString(2, ReservationStatus.CANCELLED.getLabel());
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));
            stmt.setDate(4, java.sql.Date.valueOf(checkOut));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonthStarting(int month, int year) throws SQLException {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in <= ?
            AND status IN (?, ?, ?)
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(monthStart));
            stmt.setDate(2, java.sql.Date.valueOf(monthEnd));
            stmt.setString(3, ReservationStatus.CONFIRMED.getLabel());
            stmt.setString(4, ReservationStatus.CHECKED_IN.getLabel());
            stmt.setString(5, ReservationStatus.CHECKED_OUT.getLabel());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation mapToReservation(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        int roomNumber = rs.getInt("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        RatePlan ratePlan = RatePlan.fromLabel(rs.getString("rate_plan"));
        int nights = rs.getInt("nights");
        BigDecimal subtotal = rs.getBigDecimal("subtotal");
        BigDecimal discount = rs.getBigDecimal("discount");
        BigDecimal total = rs.getBigDecimal("total");
        ReservationStatus status = ReservationStatus.fromLabel(rs.getString("status"));
        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
        BigDecimal refund = rs.getBigDecimal("refund");

        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
                status, amountPaid, refund);
    }
}
