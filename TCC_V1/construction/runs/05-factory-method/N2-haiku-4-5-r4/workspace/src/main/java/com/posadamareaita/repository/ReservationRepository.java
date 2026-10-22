package com.posadamareaita.repository;

import com.posadamareaita.database.Database;
import com.posadamareaita.domain.RatePlan;
import com.posadamareaita.domain.Reservation;
import com.posadamareaita.domain.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    public static long create(Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservations (
                    room_number, guest_name, guest_email, check_in, check_out, guests,
                    rate_plan, nights, subtotal, discount, total, status, amount_paid,
                    guest_cpf, company_cnpj, company_name, guest_passport, guest_country
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setDate(4, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(5, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(6, reservation.getGuests());
            stmt.setString(7, reservation.getRatePlan().toString());
            stmt.setInt(8, reservation.getNights());
            stmt.setBigDecimal(9, reservation.getSubtotal());
            stmt.setBigDecimal(10, reservation.getDiscount());
            stmt.setBigDecimal(11, reservation.getTotal());
            stmt.setString(12, reservation.getStatus().toString());
            stmt.setBigDecimal(13, reservation.getAmountPaid());
            stmt.setString(14, reservation.getGuestCpf());
            stmt.setString(15, reservation.getCompanyCnpj());
            stmt.setString(16, reservation.getCompanyName());
            stmt.setString(17, reservation.getGuestPassport());
            stmt.setString(18, reservation.getGuestCountry());

            stmt.executeUpdate();
            var generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            }
            throw new SQLException("Failed to get reservation ID");
        }
    }

    public static Optional<Reservation> findById(long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapReservation(resultSet));
            }
            return Optional.empty();
        }
    }

    public static void update(Reservation reservation) throws SQLException {
        String sql = """
                UPDATE reservations SET
                    status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """;
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().toString());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public static List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
                SELECT * FROM reservations
                WHERE room_number = ? AND status != 'CANCELLED'
                AND check_in < ? AND check_out > ?
                """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            var resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                reservations.add(mapReservation(resultSet));
            }
        }
        return reservations;
    }

    public static List<Reservation> findByMonthAndStatus(int year, int month) throws SQLException {
        String sql = """
                SELECT * FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ?
                AND EXTRACT(MONTH FROM check_in) = ?
                AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                ORDER BY check_in, id
                """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            var resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                reservations.add(mapReservation(resultSet));
            }
        }
        return reservations;
    }

    public static List<Reservation> findByDateAndStatus(LocalDate date) throws SQLException {
        String sql = """
                SELECT * FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ? AND check_out > ?
                ORDER BY room_number
                """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            var resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                reservations.add(mapReservation(resultSet));
            }
        }
        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation(
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getString("guest_cpf"),
                rs.getString("company_cnpj"),
                rs.getString("company_name"),
                rs.getString("guest_passport"),
                rs.getString("guest_country")
        );
        res.setId(rs.getLong("id"));
        res.setNights(rs.getInt("nights"));
        res.setSubtotal(rs.getBigDecimal("subtotal"));
        res.setDiscount(rs.getBigDecimal("discount"));
        res.setTotal(rs.getBigDecimal("total"));
        res.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        res.setAmountPaid(rs.getBigDecimal("amount_paid"));
        res.setRefund(rs.getBigDecimal("refund"));
        return res;
    }
}
