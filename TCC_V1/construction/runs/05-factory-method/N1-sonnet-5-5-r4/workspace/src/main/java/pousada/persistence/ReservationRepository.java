package pousada.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.domain.Quote;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.identity.Identity;
import pousada.identity.IdentityKinds;

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

public final class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, identity_kind, identity_data, "
            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";
    private static final String PAID = "('CONFIRMED','CHECKED_IN','CHECKED_OUT')";

    private final ObjectMapper mapper = new ObjectMapper();

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, identity_kind, identity_data, "
                + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.identity().kind().name());
            ps.setString(5, write(r.identity().fields()));
            ps.setObject(6, r.checkIn());
            ps.setObject(7, r.checkOut());
            ps.setInt(8, r.guests());
            ps.setString(9, r.ratePlan().name());
            ps.setInt(10, r.quote().nights());
            ps.setBigDecimal(11, r.quote().subtotal());
            ps.setBigDecimal(12, r.quote().discount());
            ps.setBigDecimal(13, r.quote().total());
            ps.setString(14, r.status().name());
            ps.setBigDecimal(15, r.amountPaid());
            ps.setBigDecimal(16, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong("id"));
            }
        }
    }

    public void updateState(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return findOne(c, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    /** Busca travando a linha, para que operações concorrentes na mesma reserva se enfileirem. */
    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        return findOne(c, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    /** Serializa as reservas concorrentes do mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    public boolean hasActiveOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reservas pagas que ocupam o quarto na noite, por número do quarto. */
    public List<Reservation> paidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE status IN " + PAID
                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período e deixaram dinheiro com a pousada, por entrada e número. */
    public List<Reservation> keptMoneyStartingBetween(Connection c, LocalDate from, LocalDate toExclusive)
            throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, toExclusive);
    }

    private Optional<Reservation> findOne(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    private List<Reservation> query(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(read(rs));
                }
                return result;
            }
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Identity identity = IdentityKinds.restore(rs.getString("identity_kind"), readFields(rs.getString("identity_data")));
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                identity,
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                quote,
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }

    private String write(Map<String, String> fields) {
        try {
            return mapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> readFields(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
