package com.pousadamarealta.repository;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                             int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0)",
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan);
            stmt.setInt(9, nights);
            stmt.setBigDecimal(10, subtotal);
            stmt.setBigDecimal(11, discount);
            stmt.setBigDecimal(12, total);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                int id = rs.getInt(1);
                return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                    guests, ratePlan, nights, subtotal, discount, total, "PENDING", BigDecimal.ZERO, null);
            }
        }
    }

    public Optional<Reservation> findById(int id) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void updateStatus(int id, String status) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateAmountPaid(int id, BigDecimal amountPaid) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET amount_paid = ? WHERE id = ?")) {
            stmt.setBigDecimal(1, amountPaid);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateRefund(int id, BigDecimal refund) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET refund = ? WHERE id = ?")) {
            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public boolean hasConflict(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT 1 FROM reservations WHERE room_number = ? AND status != 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1")) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Reservation> findByMonth(LocalDate monthStart, LocalDate monthEnd) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id")) {
            stmt.setDate(1, Date.valueOf(monthStart));
            stmt.setDate(2, Date.valueOf(monthEnd));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByDate(LocalDate date) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE check_in <= ? AND check_out > ? AND status != 'CANCELLED' ORDER BY room_number")) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws Exception {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(
            rs.getInt("id"),
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
            refund
        );
    }
}
