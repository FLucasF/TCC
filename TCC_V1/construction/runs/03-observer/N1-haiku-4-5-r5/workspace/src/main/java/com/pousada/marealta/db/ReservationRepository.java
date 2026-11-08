package com.pousada.marealta.db;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.enums.RatePlan;
import com.pousada.marealta.enums.ReservationStatus;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public static Reservation create(int roomNumber, String guestName, String guestEmail,
                                     String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                     int guests, RatePlan ratePlan, int nights,
                                     BigDecimal subtotal, BigDecimal discount, BigDecimal total) throws Exception {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                    "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.name());
            stmt.setInt(9, nights);
            stmt.setBigDecimal(10, subtotal);
            stmt.setBigDecimal(11, discount);
            stmt.setBigDecimal(12, total);
            stmt.setString(13, ReservationStatus.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                                         checkIn, checkOut, guests, ratePlan, nights,
                                         subtotal, discount, total, ReservationStatus.PENDING,
                                         BigDecimal.ZERO, null);
                }
            }
        }
        return null;
    }

    public static Reservation findById(long id) throws Exception {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                    "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                    "amount_paid, refund FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
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

    public static List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut,
                                                       ReservationStatus... statuses) throws Exception {
        List<Reservation> reservations = new ArrayList<>();

        StringBuilder statusList = new StringBuilder();
        for (ReservationStatus status : statuses) {
            if (statusList.length() > 0) statusList.append(", ");
            statusList.append("'").append(status.name()).append("'");
        }

        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                    "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                    "amount_paid, refund FROM reservations " +
                    "WHERE room_number = ? AND status IN (" + statusList + ") " +
                    "AND check_in < ? AND check_out > ?";

        try (Connection conn = Database.getConnection();
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

    public static void updateStatus(long id, ReservationStatus status) throws Exception {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static void updateStatusAndAmountPaid(long id, ReservationStatus status, BigDecimal amountPaid) throws Exception {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setBigDecimal(2, amountPaid);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public static void updateRefund(long id, BigDecimal refund) throws Exception {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, refund);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public static List<Reservation> findByRoomOnDate(int roomNumber, LocalDate date) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                    "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                    "amount_paid, refund FROM reservations " +
                    "WHERE room_number = ? AND status NOT IN ('CANCELLED') " +
                    "AND check_in <= ? AND check_out > ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthWithPayment(int year, int month) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                    "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                    "amount_paid, refund FROM reservations " +
                    "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
                    "AND amount_paid > 0 " +
                    "ORDER BY check_in, id";

        try (Connection conn = Database.getConnection();
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

    private static Reservation mapRow(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            refund
        );
    }
}
