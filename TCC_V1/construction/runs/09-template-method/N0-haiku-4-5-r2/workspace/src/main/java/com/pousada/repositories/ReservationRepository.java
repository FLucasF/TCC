package com.pousada.repositories;

import com.pousada.Database;
import com.pousada.models.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRepository.class);

    public static Reservation create(int roomNumber, String guestName, String guestEmail,
                                     String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                     int guests, String ratePlan, int nights,
                                     java.math.BigDecimal subtotal, java.math.BigDecimal discount,
                                     java.math.BigDecimal total) {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, " +
                "status, amount_paid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            stmt.setString(13, "PENDING");
            stmt.setBigDecimal(14, java.math.BigDecimal.ZERO);

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return findById(id);
            }
        } catch (SQLException e) {
            logger.error("Error creating reservation", e);
        }
        return null;
    }

    public static Reservation findById(int id) {
        try (Connection conn = Database.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                "amount_paid, refund FROM reservations WHERE id = " + id
            );

            if (rs.next()) {
                return mapRowToReservation(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding reservation", e);
        }
        return null;
    }

    public static void updateStatus(int id, String status) {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating reservation status", e);
        }
    }

    public static void updateAmountPaid(int id, java.math.BigDecimal amount) {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET amount_paid = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating amount paid", e);
        }
    }

    public static void updateRefund(int id, java.math.BigDecimal refund) {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET refund = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating refund", e);
        }
    }

    public static boolean hasConflict(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status != 'CANCELLED' " +
                "AND check_in < ? AND check_out > ?")) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking room availability", e);
        }
        return false;
    }

    public static List<Reservation> findForOccupancy(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                "amount_paid, refund FROM reservations " +
                "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND check_in <= ? AND check_out > ? " +
                "ORDER BY room_number")) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRowToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations for occupancy", e);
        }
        return reservations;
    }

    public static List<Reservation> findForRevenue(java.time.YearMonth month) {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        LocalDate nextMonthFirstDay = month.plusMonths(1).atDay(1);

        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, " +
                "check_out, guests, rate_plan, nights, subtotal, discount, total, status, " +
                "amount_paid, refund FROM reservations " +
                "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                "AND check_in >= ? AND check_in < ? " +
                "ORDER BY check_in, id")) {

            stmt.setDate(1, Date.valueOf(firstDay));
            stmt.setDate(2, Date.valueOf(nextMonthFirstDay));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRowToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations for revenue", e);
        }
        return reservations;
    }

    private static Reservation mapRowToReservation(ResultSet rs) throws SQLException {
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
            rs.getBigDecimal("refund")
        );
    }
}
