package br.com.marealta.infra;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    /** Serializa as reservas de um mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    public boolean hasConflict(Connection c, int roomNumber, LocalDate in, LocalDate out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, out);
            ps.setObject(3, in);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setObject(5, r.checkIn());
            ps.setObject(6, r.checkOut());
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan());
            ps.setInt(9, r.nights());
            ps.setBigDecimal(10, r.subtotal());
            ps.setBigDecimal(11, r.discount());
            ps.setBigDecimal(12, r.total());
            ps.setString(13, r.status().name());
            ps.setBigDecimal(14, r.amountPaid());
            setNullable(ps, 15, r.refund());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return r.withId(rs.getLong(1));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            setNullable(ps, 3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Reservas com check_in <= to e check_out > from, na ordem de entrada e número. */
    public List<Reservation> findOverlapping(Connection c, LocalDate from, LocalDate toExclusive) throws SQLException {
        return query(c, "check_in < ? AND check_out > ?", toExclusive, from);
    }

    public List<Reservation> findStartingBetween(Connection c, LocalDate from, LocalDate toExclusive)
            throws SQLException {
        return query(c, "check_in >= ? AND check_in < ?", from, toExclusive);
    }

    private List<Reservation> query(Connection c, String where, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM reservations WHERE " + where + " ORDER BY check_in, id")) {
            ps.setObject(1, a);
            ps.setObject(2, b);
            List<Reservation> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    private static void setNullable(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) ps.setNull(index, Types.NUMERIC);
        else ps.setBigDecimal(index, value);
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(
                rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), rs.getString("rate_plan"), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), refund);
    }
}
