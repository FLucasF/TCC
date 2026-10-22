package com.marealta.persistence;

import com.marealta.domain.Pricing;
import com.marealta.domain.RatePlan;
import com.marealta.domain.Reservation;
import com.marealta.domain.ReservationStatus;
import com.marealta.domain.Room;
import com.marealta.domain.RoomType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private static final String SELECT = """
            SELECT r.*, m.type AS room_type, m.capacity, m.nightly_rate
            FROM reservations r JOIN rooms m ON m.number = r.room_number
            """;

    public boolean isRoomTaken(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?""")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.room().number());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setDate(5, Date.valueOf(r.checkInDate()));
            ps.setDate(6, Date.valueOf(r.checkOutDate()));
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
            ps.setInt(9, r.quote().nights());
            ps.setBigDecimal(10, r.quote().subtotal());
            ps.setBigDecimal(11, r.quote().discount());
            ps.setBigDecimal(12, r.quote().total());
            ps.setString(13, r.status().name());
            ps.setBigDecimal(14, r.amountPaid());
            ps.setBigDecimal(15, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                r.assignId(keys.getLong("id"));
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
                SELECT + "WHERE r.id = ?" + (lock ? " FOR UPDATE OF r" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND r.check_in <= ? AND r.check_out > ? ORDER BY r.room_number, r.id", night, night);
    }

    public List<Reservation> findRetainingMoneyStartingBetween(Connection c, LocalDate from, LocalDate toExclusive)
            throws SQLException {
        return query(c, "WHERE r.check_in >= ? AND r.check_in < ? "
                + "AND r.amount_paid - COALESCE(r.refund, 0) > 0 ORDER BY r.check_in, r.id", from, toExclusive);
    }

    private List<Reservation> query(Connection c, String where, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + where)) {
            ps.setDate(1, Date.valueOf(a));
            ps.setDate(2, Date.valueOf(b));
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        Room room = new Room(rs.getInt("room_number"), RoomType.valueOf(rs.getString("room_type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
        Pricing.Quote quote = new Pricing.Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), room, rs.getString("guest_name"), rs.getString("guest_email"),
                rs.getString("guest_cpf"), rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
