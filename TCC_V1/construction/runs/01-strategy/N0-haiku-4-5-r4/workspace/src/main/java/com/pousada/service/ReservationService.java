package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.entity.Reservation;
import com.pousada.rateplan.PriceCalculation;
import com.pousada.rateplan.RatePlan;
import com.pousada.rateplan.RatePlanFactory;
import com.pousada.util.DateUtil;
import com.pousada.util.Money;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class ReservationService {
    private final RoomService roomService = new RoomService();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                        String companyCode) throws SQLException {
        var room = roomService.getRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.capacity) {
            throw new IllegalArgumentException(guests > room.capacity ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        RatePlan plan = RatePlanFactory.getPlan(ratePlan);
        if (plan == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!plan.isValid(checkIn, checkOut, guests, companyCode, room)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = DateUtil.getNights(checkIn, checkOut);
        PriceCalculation price = plan.calculatePrice(room, checkIn, checkOut, guests, companyCode);

        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                     guests, rate_plan, company_code, nights, subtotal, discount, total, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setDate(5, java.sql.Date.valueOf(checkIn));
            pstmt.setDate(6, java.sql.Date.valueOf(checkOut));
            pstmt.setInt(7, guests);
            pstmt.setString(8, ratePlan);
            pstmt.setString(9, companyCode);
            pstmt.setInt(10, nights);
            pstmt.setBigDecimal(11, price.subtotal());
            pstmt.setBigDecimal(12, price.discount());
            pstmt.setBigDecimal(13, price.total());

            pstmt.executeUpdate();
            var generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int id = generatedKeys.getInt(1);
                return getReservation(id);
            }
        }

        throw new SQLException("Failed to create reservation");
    }

    public Reservation getReservation(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT r.id, r.room_number, rm.type, r.guest_name, r.guest_email, r.guest_cpf, " +
                "r.check_in, r.check_out, r.guests, r.rate_plan, r.company_code, r.nights, r.subtotal, " +
                "r.discount, r.total, r.status, r.amount_paid, r.refund " +
                "FROM reservations r JOIN rooms rm ON r.room_number = rm.number WHERE r.id = " + id;
            var rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return mapReservation(rs);
            }
        }
        return null;
    }

    public Reservation confirmReservation(int id, BigDecimal amountPaid) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (!res.status.equals("PENDING")) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        String sql = "UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amountPaid);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }

        MessageService messageService = new MessageService();
        messageService.sendConfirmationEmail(res, amountPaid);

        return getReservation(id);
    }

    public Reservation cancelReservation(int id) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (!res.status.equals("PENDING") && !res.status.equals("CONFIRMED")) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        RatePlan plan = RatePlanFactory.getPlan(res.ratePlan);
        BigDecimal refund = plan.getRefundAmount(res.amountPaid);

        String sql = "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, refund);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }

        return getReservation(id);
    }

    public Reservation checkIn(int id) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (!res.status.equals("CONFIRMED")) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        String sql = "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }

        return getReservation(id);
    }

    public Reservation checkOut(int id) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (!res.status.equals("CHECKED_IN")) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        String sql = "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }

        return getReservation(id);
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT COUNT(*) FROM reservations " +
                "WHERE room_number = " + roomNumber + " " +
                "AND status != 'CANCELLED' " +
                "AND check_in < '" + checkOut + "' " +
                "AND check_out > '" + checkIn + "'";
            var rs = stmt.executeQuery(sql);
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("id"),
            rs.getInt("room_number"),
            rs.getString("room_type"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getString("company_code"),
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
