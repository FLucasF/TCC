package pousada;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** Acesso ao PostgreSQL. Cada operação de negócio roda em uma transação. */
public class Store {
    private static final String COLS = "id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, "
            + "company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, "
            + "subtotal, discount, total, status, amount_paid, refund";

    private final DataSource ds;

    public Store(DataSource ds) {
        this.ds = ds;
    }

    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public <T> T tx(Work<T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void lockRoom(Connection c, int room) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, 1_000_000L + room);
            ps.execute();
        }
    }

    public boolean isRoomBusy(Connection c, int room, LocalDate in, LocalDate out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, room);
            ps.setDate(2, Date.valueOf(out));
            ps.setDate(3, Date.valueOf(in));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, "
                        + "company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, "
                        + "nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.identification().cpf());
            ps.setString(i++, r.identification().cnpj());
            ps.setString(i++, r.identification().companyName());
            ps.setString(i++, r.identification().passport());
            ps.setString(i++, r.identification().country());
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
            ps.setNull(i, Types.NUMERIC);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return find(c, rs.getLong(1), false).orElseThrow();
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void updateState(Connection c, long id, Status status, BigDecimal amountPaid, BigDecimal refund)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, amountPaid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public List<Reservation> query(Connection c, String where, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM reservations " + where)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public void addMessage(Connection c, Message m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)",
                Statement.NO_GENERATED_KEYS)) {
            ps.setString(1, m.channel());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        }
    }

    public List<Message> messages(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
            List<Message> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return out;
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        String cpf = rs.getString("guest_cpf");
        String cnpj = rs.getString("company_cnpj");
        Identification ident = cpf != null ? new Identification.Cpf(cpf)
                : cnpj != null ? new Identification.Cnpj(cnpj, rs.getString("company_name"))
                : new Identification.Passport(rs.getString("guest_passport"), rs.getString("guest_country"));
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                ident,
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                Status.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
