package br.com.marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    private static final String COLUMNS = """
            room_number, guest_name, guest_email, identity_kind, identity_data, check_in, check_out, guests,
            rate_plan, nights, subtotal, discount, total, status, amount_paid, refund""";

    private final Database db;
    private final RoomCatalog rooms;
    private final IdentityTypes identityTypes;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcReservationRepository(Database db, RoomCatalog rooms, IdentityTypes identityTypes) {
        this.db = db;
        this.rooms = rooms;
        this.identityTypes = identityTypes;
    }

    public void insert(Reservation r) {
        String sql = "INSERT INTO reservations (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                r.assignId(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void update(Reservation r) {
        String sql = "UPDATE reservations SET room_number=?, guest_name=?, guest_email=?, identity_kind=?, "
                + "identity_data=?, check_in=?, check_out=?, guests=?, rate_plan=?, nights=?, subtotal=?, "
                + "discount=?, total=?, status=?, amount_paid=?, refund=? WHERE id=?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            ps.setLong(17, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> find(long id) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT id, " + COLUMNS + " FROM reservations WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Reservation> findAll() {
        List<Reservation> all = new ArrayList<>();
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT id, " + COLUMNS + " FROM reservations ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) all.add(read(rs));
            return all;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void bind(PreparedStatement ps, Reservation r) throws SQLException {
        ps.setInt(1, r.room().number());
        ps.setString(2, r.guestName());
        ps.setString(3, r.guestEmail());
        ps.setString(4, r.identity().type().kind());
        try {
            ps.setString(5, mapper.writeValueAsString(r.identity().data()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        ps.setDate(6, Date.valueOf(r.checkIn()));
        ps.setDate(7, Date.valueOf(r.checkOut()));
        ps.setInt(8, r.guests());
        ps.setString(9, r.ratePlan().name());
        ps.setInt(10, r.price().nights());
        ps.setBigDecimal(11, r.price().subtotal());
        ps.setBigDecimal(12, r.price().discount());
        ps.setBigDecimal(13, r.price().total());
        ps.setString(14, r.status().name());
        ps.setBigDecimal(15, r.amountPaid());
        ps.setBigDecimal(16, r.refund());
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Map<String, String> data;
        try {
            data = mapper.readValue(rs.getString("identity_data"), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        GuestIdentity identity = new GuestIdentity(identityTypes.byKind(rs.getString("identity_kind")), data);
        Price price = new Price(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), rooms.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"), rs.getString("guest_email"), identity,
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), price, ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
