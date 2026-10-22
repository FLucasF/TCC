package br.com.mare.pousada;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    private final JdbcDatabase db;

    public JdbcReservationRepository(JdbcDatabase db) {
        this.db = db;
    }

    @Override
    public Reservation insert(Reservation r) {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, identity_kind, identity_data,"
                + " check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status,"
                + " amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r, 1);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Reservation r) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setInt(4, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Reservation> find(int id) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Reservation> all = new ArrayList<>();
            while (rs.next()) {
                all.add(read(rs));
            }
            return all;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void bind(PreparedStatement ps, Reservation r, int i) throws SQLException {
        ps.setInt(i++, r.room().number());
        ps.setString(i++, r.guestName());
        ps.setString(i++, r.guestEmail());
        ps.setString(i++, IdentityKinds.kindName(r.identity()));
        ps.setString(i++, IdentityKinds.serialize(r.identity()));
        ps.setDate(i++, Date.valueOf(r.checkIn()));
        ps.setDate(i++, Date.valueOf(r.checkOut()));
        ps.setInt(i++, r.guests());
        ps.setString(i++, r.ratePlan().name());
        ps.setInt(i++, r.quote().nights());
        ps.setBigDecimal(i++, r.quote().subtotal());
        ps.setBigDecimal(i++, r.quote().discount());
        ps.setBigDecimal(i++, r.quote().total());
        ps.setString(i++, r.status().name());
        ps.setBigDecimal(i++, r.amountPaid());
        ps.setBigDecimal(i, r.refund());
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getInt("id"), RoomCatalog.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"), rs.getString("guest_email"),
                IdentityKinds.restore(rs.getString("identity_kind"), rs.getString("identity_data")),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
