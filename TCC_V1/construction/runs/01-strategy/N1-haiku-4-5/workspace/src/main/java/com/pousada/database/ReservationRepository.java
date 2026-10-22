package com.pousada.database;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail,
                             String guestCpf, LocalDate checkIn, LocalDate checkOut,
                             int guests, RatePlan ratePlan, String companyCode,
                             int nights, BigDecimal subtotal, BigDecimal discount,
                             BigDecimal total) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "check_in, check_out, guests, rate_plan, company_code, nights, subtotal, " +
                "discount, total, status, amount_paid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.toString());
            stmt.setString(9, companyCode);
            stmt.setInt(10, nights);
            stmt.setBigDecimal(11, subtotal);
            stmt.setBigDecimal(12, discount);
            stmt.setBigDecimal(13, total);
            stmt.setString(14, ReservationStatus.PENDING.toString());
            stmt.setBigDecimal(15, BigDecimal.ZERO);

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            long id = rs.getLong(1);

            return findById(id).orElseThrow();
        }
    }

    public Optional<Reservation> findById(long id) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, " +
                "check_in, check_out, guests, rate_plan, company_code, nights, " +
                "subtotal, discount, total, status, amount_paid, refund " +
                "FROM reservations WHERE id = ?")) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public void updateStatus(long id, ReservationStatus status) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, status.toString());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateAmountPaid(long id, BigDecimal amount) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET amount_paid = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, amount);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateRefund(long id, BigDecimal amount) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("UPDATE reservations SET refund = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, amount);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM reservations " +
                "WHERE room_number = ? " +
                "AND status != ? " +
                "AND check_in < ? " +
                "AND check_out > ?")) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, ReservationStatus.CANCELLED.toString());
            stmt.setDate(3, Date.valueOf(checkOut));
            stmt.setDate(4, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
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
