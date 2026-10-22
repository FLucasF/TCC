package pousada.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import pousada.domain.Identity;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

public final class ReservationRepository {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PAID_STATUSES = ReservationStatus.PAID.stream()
            .map(status -> "'" + status.name() + "'").collect(Collectors.joining(","));

    /** Serializa as reservas de um mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(2, ?)")) {
            ps.setInt(1, roomNumber);
            ps.execute();
        }
    }

    /** Há reserva não cancelada do quarto em alguma das noites de [checkIn, checkOut)? */
    public boolean isRoomTaken(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND ? < check_out
                LIMIT 1""")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, identity_type, identity_data,
                  check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status,
                  amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.identity().type());
            ps.setString(5, writeValues(r.identity().values()));
            ps.setObject(6, r.checkIn());
            ps.setObject(7, r.checkOut());
            ps.setInt(8, r.guests());
            ps.setString(9, r.ratePlan().name());
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
                return r.withId(keys.getLong(1));
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return findWhere(c, "WHERE id = ?", id);
    }

    /** Busca já bloqueando a linha, para que operações concorrentes na mesma reserva se enfileirem. */
    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        return findWhere(c, "WHERE id = ? FOR UPDATE", id);
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

    /** Reservas pagas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> findPaidOnNight(Connection c, LocalDate night) throws SQLException {
        return query(c, "WHERE status IN (" + PAID_STATUSES + ") AND check_in <= ? AND ? < check_out "
                + "ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período [from, to) e deixaram dinheiro com a pousada. */
    public List<Reservation> findRetainedStartingBetween(Connection c, LocalDate from, LocalDate to)
            throws SQLException {
        return query(c, "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                + "ORDER BY check_in, id", from, to);
    }

    private Optional<Reservation> findWhere(Connection c, String where, Object... params) throws SQLException {
        return query(c, where, params).stream().findFirst();
    }

    private List<Reservation> query(Connection c, String tail, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations " + tail)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Reservation> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                new Identity(rs.getString("identity_type"), readValues(rs.getString("identity_data"))),
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }

    private static String writeValues(Map<String, String> values) {
        try {
            return JSON.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> readValues(String json) {
        try {
            return JSON.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
