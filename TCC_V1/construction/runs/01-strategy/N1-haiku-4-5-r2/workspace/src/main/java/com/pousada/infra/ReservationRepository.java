package com.pousada.infra;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    public Reservation save(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
             rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan());
            stmt.setString(9, reservation.getCompanyCode());
            stmt.setInt(10, reservation.getNights());
            stmt.setBigDecimal(11, reservation.getSubtotal());
            stmt.setBigDecimal(12, reservation.getDiscount());
            stmt.setBigDecimal(13, reservation.getTotal());
            stmt.setString(14, reservation.getStatus().toString());
            stmt.setBigDecimal(15, reservation.getAmountPaid());
            stmt.setObject(16, reservation.getRefund());

            try (var rs = stmt.executeQuery()) {
                rs.next();
                long id = rs.getLong("id");
                return findById(id);
            }
        }
    }

    public Reservation findById(long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
        """;

        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapToReservation(rs);
                }
            }
        }
        return null;
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public void updateStatus(long id, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.toString());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateConfirmation(long id, ReservationStatus status, BigDecimal amountPaid) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.toString());
            stmt.setBigDecimal(2, amountPaid);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public void updateCancellation(long id, BigDecimal refund) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "CANCELLED");
            stmt.setBigDecimal(2, refund);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public List<Reservation> findByMonthAndPaid(int year, int month) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND amount_paid > 0
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapToReservation(java.sql.ResultSet rs) throws SQLException {
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
            rs.getString("rate_plan"),
            rs.getString("company_code"),
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
