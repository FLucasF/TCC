package com.pousadamarealta.service;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.util.PricingUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {
    public static Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) throws SQLException {
        int nights = PricingUtil.calculateNights(checkIn, checkOut);

        try (Connection conn = Database.getConnection()) {
            var roomStmt = conn.prepareStatement(
                "SELECT nightly_rate FROM rooms WHERE number = ?");
            roomStmt.setInt(1, roomNumber);
            var roomRs = roomStmt.executeQuery();

            if (!roomRs.next()) {
                throw new IllegalArgumentException("ROOM_NOT_FOUND");
            }

            BigDecimal baseRate = new BigDecimal(roomRs.getString("nightly_rate"));
            BigDecimal subtotal = PricingUtil.calculateSubtotal(baseRate, checkIn, checkOut);
            BigDecimal discount = PricingUtil.calculateDiscount(subtotal, ratePlan);
            BigDecimal total = PricingUtil.calculateTotal(subtotal, discount);

            var insertStmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0) RETURNING id",
                java.sql.Statement.RETURN_GENERATED_KEYS);

            insertStmt.setInt(1, roomNumber);
            insertStmt.setString(2, guestName);
            insertStmt.setString(3, guestEmail);
            insertStmt.setString(4, guestCpf);
            insertStmt.setObject(5, checkIn);
            insertStmt.setObject(6, checkOut);
            insertStmt.setInt(7, guests);
            insertStmt.setString(8, ratePlan);
            insertStmt.setInt(9, nights);
            insertStmt.setBigDecimal(10, subtotal);
            insertStmt.setBigDecimal(11, discount);
            insertStmt.setBigDecimal(12, total);

            insertStmt.executeUpdate();
            var rs = insertStmt.getGeneratedKeys();
            rs.next();
            int id = rs.getInt(1);

            return getReservation(id);
        }
    }

    public static Reservation getReservation(int id) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                "FROM reservations WHERE id = ?");
            stmt.setInt(1, id);
            var rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return rowToReservation(rs);
        }
    }

    public static Reservation confirm(int id, BigDecimal amountPaid) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ? AND status = 'PENDING'");
            stmt.setBigDecimal(1, amountPaid);
            stmt.setInt(2, id);

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }

            return getReservation(id);
        }
    }

    public static Reservation checkIn(int id) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ? AND status = 'CONFIRMED'");
            stmt.setInt(1, id);

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }

            return getReservation(id);
        }
    }

    public static Reservation checkOut(int id) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ? AND status = 'CHECKED_IN'");
            stmt.setInt(1, id);

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }

            return getReservation(id);
        }
    }

    public static Reservation cancel(int id) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var selectStmt = conn.prepareStatement(
                "SELECT status, rate_plan, amount_paid FROM reservations WHERE id = ?");
            selectStmt.setInt(1, id);
            var rs = selectStmt.executeQuery();

            if (!rs.next()) {
                throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
            }

            String status = rs.getString("status");
            if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }

            String ratePlan = rs.getString("rate_plan");
            BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
            BigDecimal refund = BigDecimal.ZERO;

            if ("STANDARD".equals(ratePlan) && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                refund = amountPaid;
            }

            var updateStmt = conn.prepareStatement(
                "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?");
            updateStmt.setBigDecimal(1, refund);
            updateStmt.setInt(2, id);
            updateStmt.executeUpdate();

            return getReservation(id);
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT COUNT(*) as count FROM reservations " +
                "WHERE room_number = ? " +
                "AND status NOT IN ('CANCELLED') " +
                "AND check_in < ? " +
                "AND check_out > ?");
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, checkOut);
            stmt.setObject(3, checkIn);

            var rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") == 0;
        }
    }

    private static Reservation rowToReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.id = rs.getInt("id");
        res.roomNumber = rs.getInt("room_number");
        res.guestName = rs.getString("guest_name");
        res.guestEmail = rs.getString("guest_email");
        res.guestCpf = rs.getString("guest_cpf");
        res.checkIn = rs.getObject("check_in", LocalDate.class);
        res.checkOut = rs.getObject("check_out", LocalDate.class);
        res.guests = rs.getInt("guests");
        res.ratePlan = rs.getString("rate_plan");
        res.nights = rs.getInt("nights");
        res.subtotal = rs.getBigDecimal("subtotal");
        res.discount = rs.getBigDecimal("discount");
        res.total = rs.getBigDecimal("total");
        res.status = rs.getString("status");
        res.amountPaid = rs.getBigDecimal("amount_paid");
        res.refund = rs.getBigDecimal("refund");
        return res;
    }
}
