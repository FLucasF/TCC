package br.com.maralta.pousada;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, identification, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    /** Serializa as operações sobre um quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    public boolean isRoomBooked(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM reservations WHERE room_number = ? "
                + "AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO reservations (room_number, guest_name, "
                + "guest_email, identification, check_in, check_out, guests, rate_plan, nights, subtotal, discount, "
                + "total, status, amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r, 1);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return withId(r, keys.getLong("id"));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE reservations SET status = ?, amount_paid = ?, "
                + "refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean forUpdate) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Reservas pagas que ocupam um quarto na noite, por número de quarto. */
    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN',"
                + "'CHECKED_OUT') AND check_in <= ? AND check_out > ? ORDER BY room_number, id",
                night, night);
    }

    /** Reservas com entrada no intervalo [from, to), em ordem de entrada e número. */
    public List<Reservation> findStartingBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, to);
    }

    private List<Reservation> query(Connection c, String sql, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(a));
            ps.setDate(2, Date.valueOf(b));
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    private void bind(PreparedStatement ps, Reservation r, int i) throws SQLException {
        try {
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, Json.MAPPER.writeValueAsString(r.identification().fields()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ps.setDate(i++, Date.valueOf(r.checkIn()));
        ps.setDate(i++, Date.valueOf(r.checkOut()));
        ps.setInt(i++, r.guests());
        ps.setString(i++, r.ratePlan().name());
        ps.setInt(i++, r.nights());
        ps.setBigDecimal(i++, r.subtotal());
        ps.setBigDecimal(i++, r.discount());
        ps.setBigDecimal(i++, r.total());
        ps.setString(i++, r.status().name());
        ps.setBigDecimal(i++, r.amountPaid());
        ps.setBigDecimal(i, r.refund());
    }

    private Reservation map(ResultSet rs) throws SQLException {
        try {
            Map<String, String> fields = Json.MAPPER.readValue(rs.getString("identification"),
                    new TypeReference<Map<String, String>>() { });
            return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                    rs.getString("guest_email"), Identifications.from(fields),
                    rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                    rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                    rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                    Status.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                    rs.getBigDecimal("refund"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Reservation withId(Reservation r, long id) {
        return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.identification(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                r.status(), r.amountPaid(), r.refund());
    }
}
