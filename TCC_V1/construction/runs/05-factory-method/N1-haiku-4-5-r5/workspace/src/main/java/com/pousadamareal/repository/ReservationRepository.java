package com.pousadamareal.repository;

import com.pousadamareal.db.Database;
import com.pousadamareal.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationRepository {

    public static Reservation save(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, " +
                 "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                 Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reservation.roomNumber());
            stmt.setString(2, reservation.guestName());
            stmt.setString(3, reservation.guestEmail());
            stmt.setString(4, reservation.guestCpf());
            stmt.setString(5, reservation.companyCnpj());
            stmt.setString(6, reservation.companyName());
            stmt.setString(7, reservation.guestPassport());
            stmt.setString(8, reservation.guestCountry());
            stmt.setDate(9, java.sql.Date.valueOf(reservation.checkIn()));
            stmt.setDate(10, java.sql.Date.valueOf(reservation.checkOut()));
            stmt.setInt(11, reservation.guests());
            stmt.setString(12, reservation.ratePlan().name());
            stmt.setInt(13, reservation.nights());
            stmt.setBigDecimal(14, reservation.subtotal());
            stmt.setBigDecimal(15, reservation.discount());
            stmt.setBigDecimal(16, reservation.total());
            stmt.setString(17, reservation.status().name());
            stmt.setBigDecimal(18, reservation.amountPaid());
            stmt.setObject(19, reservation.refund());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Reservation(
                        id, reservation.roomNumber(), reservation.guestName(), reservation.guestEmail(),
                        reservation.guestCpf(), reservation.companyCnpj(), reservation.companyName(),
                        reservation.guestPassport(), reservation.guestCountry(),
                        reservation.checkIn(), reservation.checkOut(), reservation.guests(),
                        reservation.ratePlan(), reservation.nights(),
                        reservation.subtotal(), reservation.discount(), reservation.total(),
                        reservation.status(), reservation.amountPaid(), reservation.refund()
                    );
                }
            }
        }
        throw new SQLException("Falha ao salvar reserva");
    }

    public static void update(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {

            stmt.setString(1, reservation.status().name());
            stmt.setBigDecimal(2, reservation.amountPaid());
            stmt.setObject(3, reservation.refund());
            stmt.setLong(4, reservation.id());
            stmt.executeUpdate();
        }
    }

    public static Optional<Reservation> findById(long id) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, " +
                 "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                 "FROM reservations WHERE id = ?")) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public static List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, " +
                 "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                 "FROM reservations WHERE room_number = ? AND status != 'CANCELLED' " +
                 "AND check_in < ? AND check_out > ?")) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByDateForOccupancy(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, " +
                 "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                 "FROM reservations WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT') " +
                 "AND check_in <= ? AND check_out > ? " +
                 "ORDER BY room_number")) {

            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthForRevenue(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, " +
                 "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                 "FROM reservations WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR status = 'CANCELLED') " +
                 "AND check_in >= ? AND check_in <= ? " +
                 "AND (amount_paid > 0 OR (status = 'CANCELLED' AND refund IS NOT NULL)) " +
                 "ORDER BY check_in, id")) {

            stmt.setDate(1, java.sql.Date.valueOf(startOfMonth));
            stmt.setDate(2, java.sql.Date.valueOf(endOfMonth));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getString("company_cnpj"),
            rs.getString("company_name"),
            rs.getString("guest_passport"),
            rs.getString("guest_country"),
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
            rs.getBigDecimal("refund")
        );
    }
}
