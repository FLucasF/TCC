package com.marealta.service;

import com.marealta.database.Database;
import com.marealta.domain.Reservation;
import com.marealta.domain.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    public static Reservation create(int roomNumber, String guestName, String guestEmail,
                                      String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                      int guests, String ratePlan, String companyCode) throws SQLException {
        Room room = RoomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        validateReservation(roomNumber, checkIn, checkOut, guests, room.capacity, ratePlan, companyCode);

        int roomId = RoomService.getRoomIdByNumber(roomNumber);
        PricingService.PricingResult pricing = calculatePricing(checkIn, checkOut, room, ratePlan, companyCode);

        String sql = """
            INSERT INTO reservations (room_id, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, company_code, nights,
                subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setObject(5, checkIn);
            stmt.setObject(6, checkOut);
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan);
            stmt.setString(9, companyCode);
            stmt.setInt(10, pricing.nights);
            stmt.setBigDecimal(11, pricing.subtotal);
            stmt.setBigDecimal(12, pricing.discount);
            stmt.setBigDecimal(13, pricing.total);
            stmt.setString(14, "PENDING");
            stmt.setBigDecimal(15, BigDecimal.ZERO);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                int id = rs.getInt("id");
                return getReservationById(id);
            }
        }
    }

    public static Reservation getReservationById(int id) throws SQLException {
        String sql = """
            SELECT r.id, r.room_id, rm.number, r.guest_name, r.guest_email, r.guest_cpf,
                   r.check_in, r.check_out, r.guests, r.rate_plan, r.company_code,
                   r.nights, r.subtotal, r.discount, r.total, r.status, r.amount_paid, r.refund
            FROM reservations r
            JOIN rooms rm ON r.room_id = rm.id
            WHERE r.id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public static void confirm(int reservationId, BigDecimal amountPaid) throws SQLException {
        String updateSql = "UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ? AND status = 'PENDING'";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setBigDecimal(1, amountPaid);
            stmt.setInt(2, reservationId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }
        }
    }

    public static void checkIn(int reservationId) throws SQLException {
        String updateSql = "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ? AND status = 'CONFIRMED'";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, reservationId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }
        }
    }

    public static void checkOut(int reservationId) throws SQLException {
        String updateSql = "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ? AND status = 'CHECKED_IN'";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, reservationId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("OPERATION_NOT_ALLOWED");
            }
        }
    }

    public static void cancel(int reservationId, String ratePlan) throws SQLException {
        Reservation res = getReservationById(reservationId);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (!res.status.equals("PENDING") && !res.status.equals("CONFIRMED")) {
            throw new IllegalStateException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = calculateRefund(res);
        String updateSql = "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, reservationId);
            stmt.executeUpdate();
        }
    }

    public static List<Reservation> getReservationsByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
        String sql = """
            SELECT r.id, r.room_id, rm.number, r.guest_name, r.guest_email, r.guest_cpf,
                   r.check_in, r.check_out, r.guests, r.rate_plan, r.company_code,
                   r.nights, r.subtotal, r.discount, r.total, r.status, r.amount_paid, r.refund
            FROM reservations r
            JOIN rooms rm ON r.room_id = rm.id
            WHERE rm.number = ?
              AND r.check_in <= ? AND r.check_out > ?
              AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, date);
            stmt.setObject(3, date);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> getReservationsByRoomInDateRange(int roomNumber, LocalDate start, LocalDate end) throws SQLException {
        String sql = """
            SELECT r.id, r.room_id, rm.number, r.guest_name, r.guest_email, r.guest_cpf,
                   r.check_in, r.check_out, r.guests, r.rate_plan, r.company_code,
                   r.nights, r.subtotal, r.discount, r.total, r.status, r.amount_paid, r.refund
            FROM reservations r
            JOIN rooms rm ON r.room_id = rm.id
            WHERE rm.number = ?
              AND r.check_in < ? AND r.check_out > ?
              AND r.status != 'CANCELLED'
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, end);
            stmt.setObject(3, start);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static void validateReservation(int roomNumber, LocalDate checkIn, LocalDate checkOut,
                                             int guests, int capacity, String ratePlan,
                                             String companyCode) throws SQLException {
        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        if (guests > capacity) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }
        if (checkOut.compareTo(checkIn) <= 0) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Validate rate plan rules
        if (ratePlan.equals("CORPORATE")) {
            if (companyCode == null || (!companyCode.equals("TECNOSUL") &&
                !companyCode.equals("AGROVALE") && !companyCode.equals("FARMAVIDA"))) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        }

        if (ratePlan.equals("WEEKEND_PACKAGE")) {
            validateWeekendPackage(checkIn, checkOut);
        }

        if (ratePlan.equals("LONG_STAY")) {
            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights < 7) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        }

        // Check room availability
        List<Reservation> conflicts = getReservationsByRoomInDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }
    }

    private static void validateWeekendPackage(LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights != 2) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        java.time.DayOfWeek checkInDay = checkIn.getDayOfWeek();
        java.time.DayOfWeek checkOutDay = checkOut.getDayOfWeek();

        if (checkInDay != java.time.DayOfWeek.FRIDAY || checkOutDay != java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }

    private static PricingService.PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut,
                                                                   Room room, String ratePlan,
                                                                   String companyCode) {
        if (ratePlan.equals("WEEKEND_PACKAGE")) {
            BigDecimal packagePrice = switch (room.type) {
                case "STANDARD" -> new BigDecimal("520.00");
                case "DELUXE" -> new BigDecimal("820.00");
                case "SUITE" -> new BigDecimal("1200.00");
                default -> BigDecimal.ZERO;
            };
            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            return new PricingService.PricingResult(nights, packagePrice, BigDecimal.ZERO, packagePrice);
        }

        return PricingService.calculatePrice(checkIn, checkOut, room.nightlyRate, ratePlan, companyCode);
    }

    private static BigDecimal calculateRefund(Reservation res) {
        switch (res.ratePlan) {
            case "STANDARD":
            case "CORPORATE":
            case "LONG_STAY":
                return res.amountPaid;
            case "WEEKEND_PACKAGE":
                return PricingService.roundMoney(res.amountPaid.multiply(new BigDecimal("0.5")));
            case "NON_REFUNDABLE":
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.id = rs.getInt("id");
        res.roomNumber = rs.getInt("number");
        res.guestName = rs.getString("guest_name");
        res.guestEmail = rs.getString("guest_email");
        res.guestCpf = rs.getString("guest_cpf");
        res.checkIn = rs.getObject("check_in", LocalDate.class);
        res.checkOut = rs.getObject("check_out", LocalDate.class);
        res.guests = rs.getInt("guests");
        res.ratePlan = rs.getString("rate_plan");
        res.companyCode = rs.getString("company_code");
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
