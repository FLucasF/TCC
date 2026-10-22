package pousada.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.domain.Quote;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Rooms;
import pousada.identity.IdentityKind;

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

public final class ReservationRepository {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, identity_kind, identity_data, "
            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    /** Serializa as gravações de um mesmo quarto, para que duas reservas não peguem a mesma noite. */
    public void lockRoom(Connection conn, int roomNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean hasOverlap(Connection conn, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection conn, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, identity_kind, identity_data, "
                + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, "
                + "refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, r.room().number());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.identity().kind().name());
            ps.setString(i++, writeFields(r.identity().fields()));
            ps.setObject(i++, r.checkIn());
            ps.setObject(i++, r.checkOut());
            ps.setInt(i++, r.guests());
            ps.setString(i++, r.ratePlan().name());
            ps.setInt(i++, r.quote().nights());
            ps.setBigDecimal(i++, r.quote().subtotal());
            ps.setBigDecimal(i++, r.quote().discount());
            ps.setBigDecimal(i++, r.quote().total());
            ps.setString(i++, r.status().name());
            ps.setBigDecimal(i++, r.amountPaid());
            ps.setBigDecimal(i, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong("id"));
            }
        }
    }

    public void update(Connection conn, Reservation r) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection conn, long id) throws SQLException {
        return queryOne(conn, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    public Optional<Reservation> findForUpdate(Connection conn, long id) throws SQLException {
        return queryOne(conn, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    /** Reservas cujas noites incluem a noite dada, em ordem de quarto. */
    public List<Reservation> occupyingNight(Connection conn, LocalDate night) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, night);
            ps.setObject(2, night);
            return readAll(ps);
        }
    }

    /** Reservas com entrada em [from, to], em ordem de entrada e número. */
    public List<Reservation> startingBetween(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in <= ? "
                + "ORDER BY check_in, id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            return readAll(ps);
        }
    }

    private Optional<Reservation> queryOne(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            List<Reservation> found = readAll(ps);
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        }
    }

    private List<Reservation> readAll(PreparedStatement ps) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(read(rs));
            }
        }
        return list;
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"));
        return new Reservation(
                rs.getLong("id"),
                Rooms.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                IdentityKind.valueOf(rs.getString("identity_kind")).create(readFields(rs.getString("identity_data"))),
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                quote,
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }

    private static String writeFields(Map<String, String> fields) {
        try {
            return JSON.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> readFields(String json) {
        try {
            return JSON.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
