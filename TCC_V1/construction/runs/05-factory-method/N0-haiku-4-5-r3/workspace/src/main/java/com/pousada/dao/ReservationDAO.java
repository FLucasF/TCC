package com.pousada.dao;

import com.pousada.config.DatabaseConfig;
import com.pousada.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDAO {
    private static final Logger logger = LoggerFactory.getLogger(ReservationDAO.class);

    public int create(Reservation reservation) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "company_cnpj, company_name, guest_passport, guest_country, check_in, check_out, " +
                "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setString(5, reservation.getCompanyCnpj());
            stmt.setString(6, reservation.getCompanyName());
            stmt.setString(7, reservation.getGuestPassport());
            stmt.setString(8, reservation.getGuestCountry());
            stmt.setDate(9, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(10, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(11, reservation.getGuests());
            stmt.setString(12, reservation.getRatePlan());
            stmt.setInt(13, reservation.getNights());
            stmt.setBigDecimal(14, reservation.getSubtotal());
            stmt.setBigDecimal(15, reservation.getDiscount());
            stmt.setBigDecimal(16, reservation.getTotal());
            stmt.setString(17, reservation.getStatus());
            stmt.setBigDecimal(18, reservation.getAmountPaid());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to generate reservation ID");
        } catch (SQLException e) {
            logger.error("Error creating reservation", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<Reservation> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error fetching reservation {}", id, e);
            throw new RuntimeException(e);
        }
    }

    public void update(Reservation reservation) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {

            stmt.setString(1, reservation.getStatus());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setInt(4, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating reservation {}", reservation.getId(), e);
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM reservations WHERE room_number = ? AND status != 'CANCELLED' " +
                "AND check_in < ? AND check_out > ? ORDER BY check_in")) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching reservations for room {} between {} and {}",
                roomNumber, checkIn, checkOut, e);
            throw new RuntimeException(e);
        }
        return reservations;
    }

    public List<Reservation> findPaidByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM reservations WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND check_in <= ? AND check_out > ? ORDER BY room_number")) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching paid reservations for date {}", date, e);
            throw new RuntimeException(e);
        }
        return reservations;
    }

    public List<Reservation> findByMonthAndStatus(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM reservations WHERE EXTRACT(YEAR FROM check_in) = ? " +
                "AND EXTRACT(MONTH FROM check_in) = ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "ORDER BY check_in, id")) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching reservations for month {}-{}", year, month, e);
            throw new RuntimeException(e);
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("id"),
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
