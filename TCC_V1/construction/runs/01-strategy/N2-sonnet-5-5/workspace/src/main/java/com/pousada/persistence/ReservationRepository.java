package com.pousada.persistence;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, "
            + "amount_paid, refund";

    /** Insere a reserva e devolve-a com o id gerado. */
    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setObject(5, r.checkIn());
            ps.setObject(6, r.checkOut());
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan());
            ps.setString(9, r.companyCode());
            ps.setInt(10, r.nights());
            ps.setBigDecimal(11, r.subtotal());
            ps.setBigDecimal(12, r.discount());
            ps.setBigDecimal(13, r.total());
            ps.setString(14, r.status().name());
            ps.setBigDecimal(15, r.amountPaid());
            ps.setBigDecimal(16, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return find(c, keys.getLong("id"), false).orElseThrow();
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ?")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reservas pagas que ocupam um quarto na noite informada. */
    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ?",
                "room_number", night, night);
    }

    /** Reservas que começam no período [from, to) e ficaram com algum dinheiro da pousada. */
    public List<Reservation> findKeepingMoney(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0",
                "check_in, id", from, to);
    }

    private List<Reservation> query(Connection c, String where, String order, LocalDate a, LocalDate b)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM reservations WHERE " + where + " ORDER BY " + order)) {
            ps.setObject(1, a);
            ps.setObject(2, b);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), rs.getString("rate_plan"), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), refund,
                rs.getString("company_code"));
    }
}
