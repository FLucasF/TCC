package br.com.marealta.persistence;

import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.identity.GuestIdentity;
import br.com.marealta.identity.IdentityKinds;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, identity_kind, identity_data, "
            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";
    private static final String PAID = "status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')";

    private final ObjectMapper mapper = new ObjectMapper();

    /** Grava uma reserva nova e devolve o número que ela recebeu. */
    public long insert(Connection connection, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, identity_kind, identity_data, "
                + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.identity().kind().code());
            ps.setString(5, writeValues(r.identity().values()));
            ps.setDate(6, Date.valueOf(r.checkIn()));
            ps.setDate(7, Date.valueOf(r.checkOut()));
            ps.setInt(8, r.guests());
            ps.setString(9, r.ratePlan().name());
            ps.setInt(10, r.quote().nights());
            ps.setBigDecimal(11, r.quote().subtotal());
            ps.setBigDecimal(12, r.quote().discount());
            ps.setBigDecimal(13, r.quote().total());
            ps.setString(14, r.status().name());
            ps.setBigDecimal(15, r.amountPaid());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, Reservation r) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    public Optional<Reservation> findForUpdate(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean existsOverlap(Connection connection, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ?")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reservas pagas que ocupam o quarto na noite indicada, por número de quarto. */
    public List<Reservation> findPaidOccupying(Connection connection, LocalDate night) throws SQLException {
        return findMany(connection, "SELECT " + COLUMNS + " FROM reservations WHERE " + PAID
                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas cuja entrada cai em [from, until), por entrada e número. */
    public List<Reservation> findCheckingInBetween(Connection connection, LocalDate from, LocalDate until)
            throws SQLException {
        return findMany(connection, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "ORDER BY check_in, id", Date.valueOf(from), Date.valueOf(until));
    }

    private Optional<Reservation> findOne(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    private List<Reservation> findMany(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> found = new ArrayList<>();
                while (rs.next()) {
                    found.add(read(rs));
                }
                return found;
            }
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        GuestIdentity identity = new GuestIdentity(IdentityKinds.byCode(rs.getString("identity_kind")),
                readValues(rs.getString("identity_data")));
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), identity, rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), quote, ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }

    private String writeValues(Map<String, String> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> readValues(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
