package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationRepository {
    private static final Logger log = LoggerFactory.getLogger(ReservationRepository.class);

    public Reservation save(Reservation reservation) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                    check_in, check_out, guests, rate_plan, company_code, nights,
                    subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                java.sql.Statement.RETURN_GENERATED_KEYS
            );

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
            stmt.setString(14, reservation.getStatus().name());
            stmt.setBigDecimal(15, reservation.getAmountPaid());
            stmt.setObject(16, reservation.getRefund());

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                return findById(id);
            }
        }
        throw new SQLException("Failed to save reservation");
    }

    public Reservation findById(long id) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf,
                       check_in, check_out, guests, rate_plan, company_code,
                       nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """
            );
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapReservation(rs);
            }
            return null;
        }
    }

    public void updateStatus(long id, Reservation.Status status) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?"
            );
            stmt.setString(1, status.name());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateConfirmation(long id, BigDecimal amountPaid) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?"
            );
            stmt.setString(1, Reservation.Status.CONFIRMED.name());
            stmt.setBigDecimal(2, amountPaid);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public void updateCancellation(long id, BigDecimal refund) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, refund = ? WHERE id = ?"
            );
            stmt.setString(1, Reservation.Status.CANCELLED.name());
            stmt.setBigDecimal(2, refund);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                """
                SELECT COUNT(*) as count FROM reservations
                WHERE room_number = ?
                AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
                AND check_in < ? AND check_out > ?
                """
            );
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") == 0;
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
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
            Reservation.Status.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
