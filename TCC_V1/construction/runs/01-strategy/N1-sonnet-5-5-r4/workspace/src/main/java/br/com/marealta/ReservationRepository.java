package br.com.marealta;

import br.com.marealta.plan.Quote;
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

public final class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, subtotal, discount, total, status, amount_paid, refund";

    /** Serializa as reservas concorrentes do mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            s.setLong(1, roomNumber);
            s.execute();
        }
    }

    public boolean hasConflict(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""")) {
            s.setInt(1, roomNumber);
            s.setDate(2, Date.valueOf(checkOut));
            s.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, NewReservation n, Quote quote) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, subtotal, discount, total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0)
                RETURNING """ + " " + COLUMNS)) {
            s.setInt(1, n.roomNumber());
            s.setString(2, n.guestName());
            s.setString(3, n.guestEmail());
            s.setString(4, n.guestCpf());
            s.setDate(5, Date.valueOf(n.checkIn()));
            s.setDate(6, Date.valueOf(n.checkOut()));
            s.setInt(7, n.guests());
            s.setString(8, n.plan().code());
            s.setBigDecimal(9, quote.subtotal());
            s.setBigDecimal(10, quote.discount());
            s.setBigDecimal(11, quote.total());
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return map(rs);
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
            s.setLong(1, id);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            s.setString(1, r.status().name());
            s.setBigDecimal(2, r.amountPaid());
            s.setBigDecimal(3, r.refund());
            s.setLong(4, r.id());
            s.executeUpdate();
        }
    }

    /** Reservas pagas que ocupam o quarto na noite, por número do quarto. */
    public List<Reservation> paidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período e deixaram dinheiro com a pousada. */
    public List<Reservation> keptMoney(Connection c, LocalDate from, LocalDate toExclusive) throws SQLException {
        return query(c, "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                + "ORDER BY check_in, id", from, toExclusive);
    }

    private List<Reservation> query(Connection c, String clause, LocalDate first, LocalDate second) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations " + clause)) {
            s.setDate(1, Date.valueOf(first));
            s.setDate(2, Date.valueOf(second));
            List<Reservation> result = new ArrayList<>();
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
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
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                refund);
    }
}
