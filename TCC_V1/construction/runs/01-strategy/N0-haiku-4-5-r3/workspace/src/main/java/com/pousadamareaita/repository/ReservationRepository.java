package com.pousadamareaita.repository;

import com.pousadamareaita.Database;
import com.pousadamareaita.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRepository.class);

    public Reservation save(Reservation reservation) {
        String sql = String.format(
            "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
            "check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid) " +
            "VALUES (%d, '%s', '%s', '%s', '%s', '%s', %d, '%s', %s, %d, %s, %s, %s, '%s', %s) " +
            "RETURNING id",
            reservation.getRoomNumber(),
            escapeSql(reservation.getGuestName()),
            escapeSql(reservation.getGuestEmail()),
            escapeSql(reservation.getGuestCpf()),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan(),
            reservation.getCompanyCode() != null ? "'" + escapeSql(reservation.getCompanyCode()) + "'" : "NULL",
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus(),
            reservation.getAmountPaid()
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                reservation.setId(rs.getLong("id"));
                return reservation;
            }
        } catch (SQLException e) {
            logger.error("Error saving reservation", e);
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Failed to save reservation");
    }

    public Optional<Reservation> findById(long id) {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
                     "amount_paid, refund FROM reservations WHERE id = " + id;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return Optional.of(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching reservation", e);
        }

        return Optional.empty();
    }

    public void update(Reservation reservation) {
        String sql = String.format(
            "UPDATE reservations SET status = '%s', amount_paid = %s, refund = %s WHERE id = %d",
            reservation.getStatus(),
            reservation.getAmountPaid(),
            reservation.getRefund() != null ? reservation.getRefund().toPlainString() : "NULL",
            reservation.getId()
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("Error updating reservation", e);
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = String.format(
            "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
            "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
            "amount_paid, refund FROM reservations " +
            "WHERE room_number = %d AND check_in <= '%s' AND check_out > '%s' AND status != 'CANCELLED'",
            roomNumber, date, date
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching reservations for room and date", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int month, int year) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = String.format(
            "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
            "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
            "amount_paid, refund FROM reservations " +
            "WHERE EXTRACT(MONTH FROM check_in) = %d AND EXTRACT(YEAR FROM check_in) = %d " +
            "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
            "ORDER BY check_in, id",
            month, year
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching reservations for month", e);
        }

        return reservations;
    }

    public List<Reservation> findByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = String.format(
            "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
            "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
            "amount_paid, refund FROM reservations " +
            "WHERE check_in <= '%s' AND check_out > '%s' AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')" +
            "ORDER BY room_number",
            date, date
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching reservations for date", e);
        }

        return reservations;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getInt("room_number"),
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
            rs.getBigDecimal("total")
        );

        reservation.setId(rs.getLong("id"));
        reservation.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        java.math.BigDecimal refund = rs.getBigDecimal("refund");
        if (refund != null) {
            reservation.setRefund(refund);
        }

        return reservation;
    }

    private String escapeSql(String str) {
        if (str == null) return "";
        return str.replace("'", "''");
    }
}
